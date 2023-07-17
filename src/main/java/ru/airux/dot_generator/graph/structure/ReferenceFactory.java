package ru.airux.dot_generator.graph.structure;

public class ReferenceFactory {
    public static String create(String namespace, String className) {
        return namespace + "\\" + className;
    }
}
