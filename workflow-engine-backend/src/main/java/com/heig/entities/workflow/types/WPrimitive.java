package com.heig.entities.workflow.types;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public enum WPrimitive implements WType {
    Integer(0),
    String(""),
    Boolean(true),
    Byte((byte) 0),
    Short((short) 0),
    Long((long) 0),
    Float(0.0f),
    Double(0.0),
    Character((char) 0);

    private final Object defaultValue;
    WPrimitive(@Nonnull Object defaultValue) {
        this.defaultValue = Objects.requireNonNull(defaultValue);
    }

    @Override
    public boolean canBeConvertedFrom(@Nonnull WType other) {
        Objects.requireNonNull(other);
        if (other instanceof WPrimitive primitive) {
            return this == primitive;
        }
        return false;
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public Object defaultValue() {
        return defaultValue;
    }
}
