package ru.airux.dot_generator.graph;

import java.util.List;
import java.util.*;

public class DotToSvgGraphManager implements GraphManager {

    private final Map<Element, Node> elements = new HashMap<>();

    @Override
    public void setRootElement(Element element) {
        elements.put(element, new Node(element));
    }

    @Override
    public void addInheritance(Element subject, Element object) {
        if (!elements.containsKey(object)) {
            throw new RuntimeException("No target");
        }

        if (!elements.containsKey(subject)) {
            elements.put(subject, new Node(subject));
        }

        if (!elements.get(object).inheritanceElements.contains(subject)) {
            elements.get(object).inheritanceElements.add(subject);
        }
    }

    @Override
    public void addDependent(Element subject, Element object) {
        if (!elements.containsKey(object)) {
            throw new RuntimeException("No target");
        }

        if (!elements.containsKey(subject)) {
            elements.put(subject, new Node(subject));
        }

        if (!elements.get(object).dependentElements.contains(subject)) {
            elements.get(object).dependentElements.add(subject);
        }
    }

    @Override
    public String getDot() {
        var stringBuilder = new StringBuilder();
        stringBuilder.append("digraph {");
        stringBuilder.append("nodesep=.05;\n");
        stringBuilder.append("node [shape=record];\n");
        elements.forEach((element, node) -> {
            stringBuilder
                    .append(element.identifier())
                    .append("  [label = \"{")
                    .append(node.element.title());
            stringBuilder.append("}\"]").append(";\n");
            node.inheritanceElements.forEach(inheritanceElement -> {
                stringBuilder.append(element.identifier());
                stringBuilder.append("->");
                stringBuilder.append(inheritanceElement.identifier());
                stringBuilder.append("[dir=back,arrowtail=\"empty\"]");
                stringBuilder.append(";\n");
            });
            node.dependentElements.forEach(dependentElement -> {
                stringBuilder.append(element.identifier());
                stringBuilder.append("->");
                stringBuilder.append(dependentElement.identifier());
                stringBuilder.append("[dir=back,arrowtail=\"odiamond\"]");
                stringBuilder.append(";\n");
            });
        });
        stringBuilder.append("}");

        return stringBuilder.toString();
    }

    private static class Node {
        private final Element element;
        private final List<Element> dependentElements = new ArrayList<>();

        private final List<Element> inheritanceElements = new ArrayList<>();

        private Node(Element element) {
            this.element = element;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(element, node.element) && Objects.equals(dependentElements, node.dependentElements) && Objects.equals(inheritanceElements, node.inheritanceElements);
        }

        @Override
        public int hashCode() {
            return Objects.hash(element, dependentElements, inheritanceElements);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "element=" + element +
                    ", dependentElements=" + dependentElements +
                    ", inheritanceElements=" + inheritanceElements +
                    '}';
        }
    }
}
