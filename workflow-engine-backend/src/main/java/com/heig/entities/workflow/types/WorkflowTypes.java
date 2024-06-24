package com.heig.entities.workflow.types;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class WorkflowTypes {
    private static WType determineCommonTypeOf(@Nonnull Stream<WType> stream) {
        Objects.requireNonNull(stream);
        return stream.reduce((acc, t) -> {
            //Here we can determine that the type is Object with Object and Integer as parameters for example
            if (acc.canBeConvertedFrom(t)) {
                return acc;
            }
            if (t.canBeConvertedFrom(acc)) {
                return t;
            }
            //Here we need to check more thoroughly if we have 2 lists or 2 maps. For example, if we have a List<Int> and
            //List<String> we want the type to be List<Object>. If this step was omitted, we would have returned Object.
            if (acc instanceof WCollection collectionAcc && t instanceof WCollection collectionT) {
                return WCollection.of(determineCommonTypeOf(Stream.of(collectionAcc.getValueType(), collectionT.getValueType())));
            }
            if (acc instanceof WMap mapAcc && t instanceof WMap mapT) {
                return WMap.of(
                    determineCommonTypeOf(Stream.of(mapAcc.getKeyType(), mapT.getKeyType())),
                    determineCommonTypeOf(Stream.of(mapAcc.getValueType(), mapT.getValueType()))
                );
            }
            return WObject.of();
        }).orElse(WObject.of());
    }

    public static WType fromObject(@Nonnull Object o) {
        Objects.requireNonNull(o);
        if (o instanceof Collection<?> collection) {
            var valueType = determineCommonTypeOf(collection.stream().map(WorkflowTypes::fromObject));
            return WCollection.of(valueType);
        }
        if (o instanceof Map<?, ?> map) {
            var keyType = determineCommonTypeOf(map.keySet().stream().map(WorkflowTypes::fromObject));
            var valueType = determineCommonTypeOf(map.values().stream().map(WorkflowTypes::fromObject));
            return WMap.of(keyType, valueType);
        }
        if (o instanceof Integer) {
            return WPrimitive.Integer;
        }
        if (o instanceof String) {
            return WPrimitive.String;
        }
        if (o instanceof Boolean) {
            return WPrimitive.Boolean;
        }
        if (o instanceof Byte) {
            return WPrimitive.Byte;
        }
        if (o instanceof Short) {
            return WPrimitive.Short;
        }
        if (o instanceof Long) {
            return WPrimitive.Long;
        }
        if (o instanceof Float) {
            return WPrimitive.Float;
        }
        if (o instanceof Double) {
            return WPrimitive.Double;
        }
        if (o instanceof Character) {
            return WPrimitive.Character;
        }
        return WObject.of();
    }
}
