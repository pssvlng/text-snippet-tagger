package com.snippettray.model;

public record Tag(int id, String name) {
    @Override
    public String toString() {
        return name;
    }
}
