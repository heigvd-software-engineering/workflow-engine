package com.heig.entities.workflow.types;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WCollection implements WType {
    private static final ConcurrentMap<WType, WCollection> cache = new ConcurrentHashMap<>();
    private final WType valueType;

    private WCollection(WType valueType) {
        this.valueType = valueType;
    }

    public WType getValueType() {
        return valueType;
    }

    public static WCollection of(WType valueType) {
        return cache.computeIfAbsent(valueType, WCollection::new);
    }

    @Override
    public boolean canBeConvertedFrom(WType other) {
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
    public Object defaultValue() {
        return List.of();
    }
}
