package com.github.standingsconverter.entity;

public class Problem {
    private final char id;
    private final String name;

    public Problem(char id, String name) {
        this.id = id;
        this.name = name;
    }

    public char getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
