package ru.airux.dot_generator;

import ru.airux.dot_generator.graph.DotToSvgGraphManager;
import ru.airux.dot_generator.graph.Element;
import ru.airux.dot_generator.graph.GraphManager;
import ru.airux.dot_generator.graph.SimpleElement;
import ru.airux.dot_generator.graph.structure.Node;
import ru.airux.dot_generator.graph.structure.TokensToStructureAdapter;

import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class Main {
    private static int maxDepth = 4;
    public static int i = 0;
    private static final GraphManager graphManager = new DotToSvgGraphManager();
    private static Map<String, Node> references;
    private static final Map<String, List<String>> implementReferences = new HashMap<>();
    private static final Map<String, List<String>> inheritanceReferences = new HashMap<>();
    private static final Map<String, Element> elementHeap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        var files = FilesCollector.getFiles(args[0]);
        var rootReference = args[1];
        if (args.length > 2) {
            maxDepth = Integer.parseInt(args[2]);
        }
        references = new HashMap<>();
        files.forEach(item -> {
            try {
                var node = TokensToStructureAdapter.adapt(item);
                if (references.containsKey(node.reference())) {
                    return;
                } else {
                    references.put(node.reference(), node);
                }

                node.extensionReferences().forEach(ref -> {
                    if (!inheritanceReferences.containsKey(ref)) {
                        inheritanceReferences.put(ref, new LinkedList<>());
                    }

                    inheritanceReferences.get(ref).add(node.reference());
                });

                node.implementReferences().forEach(ref -> {
                    if (!implementReferences.containsKey(ref)) {
                        implementReferences.put(ref, new LinkedList<>());
                    }

                    implementReferences.get(ref).add(node.reference());
                });
            } catch (Throwable ignored) {}
        });

        var rootNode = references.get(rootReference);
        var root = getOrCreateElement(rootNode);
        graphManager.setRootElement(root);

        walk(rootNode, 0);

        System.out.println(graphManager.getDot());
    }

    private static void walk(Node node, int depth) {
        if (depth > maxDepth) {
            return;
        }

        node.properties().stream().map(Node.Property::reference).filter(Objects::nonNull).forEach(new DependentConsumer(node, depth));
        if (implementReferences.containsKey(node.reference())) {
            implementReferences.get(node.reference()).stream()
                    .filter(ref -> references.containsKey(ref))
                    .forEach(ref -> {
                        graphManager.addInheritance(getOrCreateElement(references.get(ref)), getOrCreateElement(node));
                        walk(references.get(ref), depth + 1);
                    });
        }
        if (inheritanceReferences.containsKey(node.reference())) {
            inheritanceReferences.get(node.reference()).stream()
                    .filter(ref -> references.containsKey(ref))
                    .forEach(ref -> {
                        graphManager.addInheritance(getOrCreateElement(references.get(ref)), getOrCreateElement(node));
                        walk(references.get(ref), depth + 1);
                    });
        }
    }

    private static Element getOrCreateElement(Node node) {
        if (!elementHeap.containsKey(node.reference())) {
            elementHeap.put(node.reference(), new SimpleElement(node.name()));
        }
        return elementHeap.get(node.reference());
    }

    private static Element getOrCreateElement(String ref) {
        if (!elementHeap.containsKey(ref)) {
            var parts = ref.split("\\\\");
            elementHeap.put(ref, new SimpleElement(parts[parts.length - 1]));
        }
        return elementHeap.get(ref);
    }

    private record DependentConsumer(Node node, int depth) implements Consumer<String> {
        @Override
        public void accept(String ref) {
            if (references.containsKey(ref)) {
                var refNode = references.get(ref);
                graphManager.addDependent(getOrCreateElement(refNode), getOrCreateElement(node));
                walk(refNode, depth + 1);
            } else {
                graphManager.addDependent(getOrCreateElement(ref), getOrCreateElement(node));
            }
        }
    }
}