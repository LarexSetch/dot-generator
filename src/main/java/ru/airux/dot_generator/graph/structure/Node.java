package ru.airux.dot_generator.graph.structure;

import java.io.File;
import java.util.List;

public record Node(File file,
                   String namespace,
                   String name,
                   Type type,
                   List<Property> properties,
                   List<String> implementReferences,
                   List<String> extensionReferences
) {
    public String reference() {
        return ReferenceFactory.create(namespace, name);
    }

    public enum Type {CLASS, INTERFACE}

    public record Property(String name, String reference) {
    }
}
