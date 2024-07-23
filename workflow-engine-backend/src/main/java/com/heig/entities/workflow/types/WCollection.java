package com.heig.entities.workflow.types;

import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Type representing a list or a collection
 */
public class WCollection implements WIterableType {
    private static final ConcurrentMap<WIterableType, WCollection> cache = new ConcurrentHashMap<>();

    /**
     * The type of the values
     */
    private final WIterableType valueType;

    private WCollection(@Nonnull WIterableType valueType) {
        this.valueType = Objects.requireNonNull(valueType);
    }

    public WIterableType getValueType() {
        return valueType;
    }

    /**
     * Creates a type for a list with values specified by valueType
     * @param valueType The type of the list values
     * @return The list type
     */
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
    public int getHashCode(@Nonnull Object value) {
        if (value instanceof List<?> collection) {
            return Arrays.hashCode(
                collection
                    .stream()
                    .map(valueType::getHashCode)
                    .toArray()
            );
        }
        throw new RuntimeException("WCollection should always be an instance of List");
    }

    @Override
    public Collection<?> defaultValue() {
        return List.of();
    }
}
