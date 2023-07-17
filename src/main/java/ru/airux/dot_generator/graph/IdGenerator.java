package ru.airux.dot_generator.graph;

import java.util.concurrent.atomic.AtomicInteger;

class IdGenerator {
    private static final AtomicInteger counter = new AtomicInteger();
    static int nextValue() {
        return counter.getAndIncrement();
    }
}
