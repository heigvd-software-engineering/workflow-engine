package com.heig.entities.workflow.types;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public class WObject implements WIterableType {
    private static final WObject instance = new WObject();

    private WObject() {}

    public static WObject of() {
        return instance;
    }

    @Override
    public boolean canBeConvertedFrom(@Nonnull WType other) {
        Objects.requireNonNull(other);
        return true;
    }

    @Override
    public String toString() {
        return "Object";
    }

    @Override
    public Object defaultValue() {
        return new Object();
    }
}
