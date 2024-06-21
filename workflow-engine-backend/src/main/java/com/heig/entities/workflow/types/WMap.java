package com.heig.entities.workflow.types;

import io.smallrye.mutiny.tuples.Tuple2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WMap implements WType {
    private static final ConcurrentMap<Tuple2<WType, WType>, WMap> cache = new ConcurrentHashMap<>();
    private final WType keyType, valueType;

    private WMap(Tuple2<WType, WType> types) {
        this.keyType = types.getItem1();
        this.valueType = types.getItem2();
    }

    public WType getKeyType() {
        return keyType;
    }

    public WType getValueType() {
        return valueType;
    }

    public static WMap of(WType keyType, WType valueType) {
        return cache.computeIfAbsent(Tuple2.of(keyType, valueType), WMap::new);
    }

    @Override
    public boolean canBeConvertedFrom(WType other) {
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
    public Object defaultValue() {
        return Map.of();
    }
}