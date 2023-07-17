package ru.airux.dot_generator.graph;

public interface GraphManager {
    /**
     * This element sets as level 0
     */
    void setRootElement(Element element);

    /**
     * <b>object</b> Element extends or implements <b>subject</b> Element
     */
    void addInheritance(Element subject, Element object);

    /**
     * <b>object</b> Element depends on <b>subject</b> Element
     */
    void addDependent(Element subject, Element object);

    String getDot();
}
