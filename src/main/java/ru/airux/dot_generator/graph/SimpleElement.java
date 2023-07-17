package ru.airux.dot_generator.graph;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SimpleElement implements Element {
    private final String title;
    private final int id;

    public SimpleElement(String title) {
        this.id = IdGenerator.nextValue();
        this.title = title;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String identifier() {
        return "node" + id;
    }

    @Override
    public String description() {
        return "";
    }

    @Override
    public List<Node> nodes() {
        return Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleElement that = (SimpleElement) o;
        return Objects.equals(title, that.title) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, id);
    }

    @Override
    public String toString() {
        return "SimpleElement{" +
                "title='" + title + '\'' +
                ", id=" + id +
                '}';
    }
}
