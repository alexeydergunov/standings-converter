package com.github.standingsconverter.entity;

public class Team {
    private final int id;
    private final String name;

    public Team(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
