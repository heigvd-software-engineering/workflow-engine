package com.heig.entities.workflow.types;

import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Type representing a map
 */
public class WMap implements WIterableType {
    private static final ConcurrentMap<Tuple2<WIterableType, WIterableType>, WMap> cache = new ConcurrentHashMap<>();

    /**
     * The key and value types
     */
    private final WIterableType keyType, valueType;

    private WMap(@Nonnull Tuple2<WIterableType, WIterableType> types) {
        Objects.requireNonNull(types);
        this.keyType = Objects.requireNonNull(types.getItem1());
        this.valueType = Objects.requireNonNull(types.getItem2());
    }

    public WIterableType getKeyType() {
        return keyType;
    }

    public WIterableType getValueType() {
        return valueType;
    }

    /**
     * Creates a type for a map having keyType type as key and valueType type as value
     * @param keyType The type of the type
     * @param valueType The type of the value
     * @return The type
     */
    public static WMap of(@Nonnull WIterableType keyType, @Nonnull WIterableType valueType) {
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        return cache.computeIfAbsent(Tuple2.of(keyType, valueType), WMap::new);
    }

    @Override
    public boolean canBeConvertedFrom(@Nonnull WType other) {
        Objects.requireNonNull(other);
        if (other instanceof WMap map) {
            return keyType.canBeConvertedFrom(map.keyType) && valueType.canBeConvertedFrom(map.valueType);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Map<%s, %s>".formatted(keyType.toString(), valueType.toString());
    }

    @Override
    public int getHashCode(@Nonnull Object value) {
        if (value instanceof Map<?, ?> map) {
            return Arrays.hashCode(
                map
                    .entrySet()
                    .stream()
                    .map(entry ->
                        Objects.hash(
                            keyType.getHashCode(entry.getKey()),
                            valueType.getHashCode(entry.getValue())
                        )
                    )
                    .toArray()
            );
        }
        throw new RuntimeException("WMap should always be an instance of Map");
    }

    @Override
    public Map<?, ?> defaultValue() {
        return Map.of();
    }
}
