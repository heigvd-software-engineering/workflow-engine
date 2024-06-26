package com.heig.entities.workflow.types;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class WorkflowTypes {
    private static WIterableType determineCommonTypeOf(@Nonnull Stream<WIterableType> stream) {
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

    private static Stream<WIterableType> ensureAllAreIterableTypes(Stream<WType> stream) {
        var wIterableList = stream.map(wType -> {
            if (wType instanceof WIterableType w) {
                return w;
            }
            return null;
        }).toList();
        if (wIterableList.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Collection contains non-iterable type !");
        }
        return wIterableList.stream();
    }

    public static WType fromObject(@Nonnull Object o) {
        Objects.requireNonNull(o);
        if (o instanceof Collection<?> collection) {
            var wTypeStream = collection.stream().map(WorkflowTypes::fromObject);
            var valueType = determineCommonTypeOf(ensureAllAreIterableTypes(wTypeStream));
            return WCollection.of(valueType);
        }
        if (o instanceof Map<?, ?> map) {
            var keyWTypeStream = map.keySet().stream().map(WorkflowTypes::fromObject);
            var valuesWTypeStream = map.values().stream().map(WorkflowTypes::fromObject);

            var keyType = determineCommonTypeOf(ensureAllAreIterableTypes(keyWTypeStream));
            var valueType = determineCommonTypeOf(ensureAllAreIterableTypes(valuesWTypeStream));

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
        if (o instanceof WFlow) {
            return WFlow.of();
        }
        return WObject.of();
    }
}
