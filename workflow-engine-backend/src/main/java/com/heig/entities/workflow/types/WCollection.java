package com.heig.entities.workflow.types;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WCollection implements WIterableType {
    private static final ConcurrentMap<WIterableType, WCollection> cache = new ConcurrentHashMap<>();
    private final WIterableType valueType;

    private WCollection(@Nonnull WIterableType valueType) {
        this.valueType = Objects.requireNonNull(valueType);
    }

    public WIterableType getValueType() {
        return valueType;
    }

    public static WCollection of(@Nonnull WIterableType valueType) {
        return cache.computeIfAbsent(valueType, WCollection::new);
    }

    @Override
    public boolean canBeConvertedFrom(@Nonnull WType other) {
        Objects.requireNonNull(other);
        if (other instanceof WCollection collection) {
            return valueType.canBeConvertedFrom(collection.valueType);
        }
        return false;
    }

    @Override
    public String toString() {
        return "List<%s>".formatted(valueType.toString());
    }

    @Override
    public Collection<?> defaultValue() {
        return List.of();
    }
}
