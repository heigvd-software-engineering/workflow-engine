package com.heig.entities.workflowTypes;

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
    public boolean isCompatibleWith(WType other) {
        if (other instanceof WObject) {
            return true;
        }
        if (other instanceof WCollection collection) {
            return valueType.isCompatibleWith(collection.valueType);
        }
        return false;
    }
}
