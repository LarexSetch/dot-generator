package ru.airux.dot_generator.graph;

import java.util.List;

public interface Element {
    String title();

    String identifier();

    String description();

    List<Node> nodes();

    public interface Node {
        String identifier();
        String value();
        List<String> tags();
    }
}
