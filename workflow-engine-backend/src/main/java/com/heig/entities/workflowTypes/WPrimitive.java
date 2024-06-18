package com.heig.entities.workflowTypes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WPrimitive implements WType {
    private static final ConcurrentMap<WPrimitiveTypes, WPrimitive> cache = new ConcurrentHashMap<>();
    private final WPrimitiveTypes valueType;

    private WPrimitive(WPrimitiveTypes valueType) {
        this.valueType = valueType;
    }

    public WPrimitiveTypes getValueType() {
        return valueType;
    }

    public static WPrimitive of(WPrimitiveTypes valueType) {
        return cache.computeIfAbsent(valueType, c -> new WPrimitive(valueType));
    }

    @Override
    public boolean isCompatibleWith(WType other) {
        if (other instanceof WObject) {
            return true;
        }
        if (other instanceof WPrimitive primitive) {
            return valueType == primitive.valueType;
        }
        return true;
    }
}
