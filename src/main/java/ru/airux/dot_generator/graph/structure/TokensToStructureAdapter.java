package ru.airux.dot_generator.graph.structure;

import ru.airux.lexer.php.reader.FileReader;
import ru.airux.lexer.php.token.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class TokensToStructureAdapter {
    public static Node adapt(File file) throws IOException {
        var navigator = new TokensNavigator(readTokens(file));
        var namespace = findNamespace(navigator);
        if (null == namespace) {
            throw new RuntimeException("Namespace not found in " + file);
        }

        var useReferences = findPhpUseReferences(navigator);
        var phpClassNavigator = navigator.findFirst(PhpClass.class);
        if (!phpClassNavigator.has()) {
            var phpInterfaceNavigator = navigator.findFirst(PhpInterface.class);
            if (!phpInterfaceNavigator.has()) {
                throw new RuntimeException("Class and Interface not found in " + file);
            }
            var extensionReferences = new LinkedList<String>();
            phpInterfaceNavigator.token.getChild().forEach(token -> {
                if (token instanceof PhpClassExtends) {
                    if (useReferences.containsKey(((PhpClassExtends) token).statement())) {
                        extensionReferences.add(useReferences.get(((PhpClassExtends) token).statement()));
                    }
                }
            });

            return new Node(
                    file,
                    namespace.statement(),
                    phpInterfaceNavigator.token.name(),
                    Node.Type.INTERFACE,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    extensionReferences
            );
        }

        var implementReferences = new LinkedList<String>();
        var extensionReferences = new LinkedList<String>();
        phpClassNavigator.token.getChild().forEach(token -> {
            if (token instanceof PhpClassExtends) {
                var extStatement = ((PhpClassExtends) token).statement();
                if (useReferences.containsKey(extStatement)) {
                    extensionReferences.add(useReferences.get(((PhpClassExtends) token).statement()));
                } else {
                    extensionReferences.add(namespace.statement() + "\\\\" + extStatement);
                }
            }
            if (token instanceof PhpClassImplementation) {
                var implStatement = ((PhpClassImplementation) token).statement();
                if (useReferences.containsKey(implStatement)) {
                    implementReferences.add(useReferences.get(((PhpClassImplementation) token).statement()));
                } else {
                    implementReferences.add(namespace.statement() + "\\" + implStatement);
                }
            }
        });

        var properties = findProperties(new TokensNavigator(phpClassNavigator.token.getChild()));
        return new Node(
                file,
                namespace.statement(),
                phpClassNavigator.token.name(),
                Node.Type.INTERFACE,
                createProperties(namespace, useReferences, properties),
                implementReferences,
                extensionReferences
        );
    }

    private static List<Token> readTokens(File file) throws IOException {
        var inputStream = new FileInputStream(file);
        var fileReader = new FileReader(inputStream);
        return PhpFileReader.read(fileReader);
    }

    private static List<Node.Property> createProperties(PhpNamespace namespace, Map<String, String> useReferences, List<Property> properties) {
        return properties.stream()
                .map(property -> {
                    if (null != property.phpDocVar && useReferences.containsKey(property.phpDocVar.type())) {
                        return new Node.Property(property.property.name(), useReferences.get(property.phpDocVar.type()));
                    } else if (null != property.phpDocVar) {
                        return new Node.Property(property.property.name(), namespace.statement() + "\\" + property.phpDocVar.type());
                    }

                    return new Node.Property(property.property.name(), null);
                })
                .toList();
    }

    private static List<Property> findProperties(TokensNavigator navigator) {
        var result = new LinkedList<Property>();
        TokenNavigator<?> item = navigator.findFirst(PhpClassProperty.class);
        do {
            if (!item.is(PhpClassProperty.class)) {
                item = item.next();
                continue;
            }

            var property = new Property();
            property.property = (PhpClassProperty) item.token;
            property.phpDocVar = findPhpDocVar((TokenNavigator<PhpClassProperty>) item);

            result.add(property);
            item = item.next();
        } while (item.has());

        return result;
    }

    private static PhpDocVar findPhpDocVar(TokenNavigator<PhpClassProperty> propertyNavigator) {
        TokenNavigator<?> item = propertyNavigator.prev();
        do {
            if (item.is(PhpDoc.class)) {
                var phpDocVar = item.findFirst(PhpDocVar.class);
                if (phpDocVar.has()) {
                    return phpDocVar.token;
                }
            }

            item = item.prev();
        } while (item.has() && !item.is(PhpClassConstant.class) && !item.is(PhpClassProperty.class) && !item.is(PhpClassFunctionBody.class));

        return null;
    }

    private static Map<String, String> findPhpUseReferences(TokensNavigator navigator) {
        var result = new HashMap<String, String>();
        TokenNavigator<?> item = navigator.findFirst(PhpUse.class);

        do {
            if (item.is(PhpUse.class)) {
                var phpUse = (PhpUse) item.token;
                if (null != phpUse.alias() && !phpUse.alias().isEmpty()) {
                    result.put(phpUse.alias(), phpUse.statement());
                }

                var nodes = phpUse.statement().split("\\\\");
                result.put(nodes[nodes.length - 1], phpUse.statement());
            }
            item = item.next();
        } while (item.has());

        return result;
    }

    private static PhpNamespace findNamespace(TokensNavigator navigator) {
        return navigator.findFirst(PhpNamespace.class).token;
    }

    private static class TokensNavigator {
        private final List<Token> tokens;

        TokensNavigator(List<Token> tokens) {
            this.tokens = tokens;
        }

        public <T extends Token> TokenNavigator<T> findFirst(Class<T> type) {
            return deepWalk(tokens, type);
        }

        private <T extends Token> TokenNavigator<T> deepWalk(List<Token> tokens, Class<T> type) {
            for (var token : tokens) {
                if (type.isAssignableFrom(token.getClass())) {
                    return new TokenNavigator<>((T) token, tokens);
                }

                var child = deepWalk(token.getChild(), type);
                if (child.has()) {
                    return child;
                }
            }

            return new TokenNavigator<>(null, Collections.emptyList());
        }
    }

    private record PhpUseNode(String fileReference, String reference) {

    }

    private static class TokenNavigator<T extends Token> {
        private final T token;
        private final List<Token> tokens;
        private int index = 0;

        TokenNavigator(T token, List<Token> tokens) {
            this.token = token;
            this.tokens = tokens;
            for (var i = 0; i < tokens.size(); i++) {
                if (tokens.get(i) == token) {
                    index = i;
                }
            }
        }

        TokenNavigator(T token, List<Token> tokens, int index) {
            this.token = token;
            this.tokens = tokens;
            this.index = index;
        }

        public <V extends Token> TokenNavigator<V> findFirst(Class<V> type) {
            return new TokensNavigator(token.getChild()).findFirst(type);
        }

        public T token() {
            return token;
        }

        public <V> boolean is(Class<V> type) {
            if (null == token) {
                return false;
            }

            return type.isAssignableFrom(token.getClass());
        }

        public boolean has() {
            return null != token;
        }

        public TokenNavigator<?> next() {
            if (0 <= index + 1 && index + 1 < tokens.size()) {
                return new TokenNavigator<>(tokens.get(index + 1), tokens);
            }

            return new TokenNavigator<>(null, tokens, -1);
        }

        public TokenNavigator<?> prev() {
            if (0 <= index - 1 && index - 1 < tokens.size()) {
                return new TokenNavigator<>(tokens.get(index - 1), tokens);
            }

            return new TokenNavigator<>(null, tokens, tokens.size());
        }
    }

    private static class Property {
        private PhpClassProperty property;
        private PhpDocVar phpDocVar;

        @Override
        public String toString() {
            return "Property{" +
                    "property=" + property +
                    ", phpDocVar=" + phpDocVar +
                    '}';
        }
    }
}
