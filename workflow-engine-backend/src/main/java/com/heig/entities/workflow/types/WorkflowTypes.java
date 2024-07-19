package com.heig.entities.workflow.types;

import com.heig.entities.workflow.file.FileWrapper;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
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

    /**
     * Used to have the same type for every execution.
     * Example before the function was introduced: 1st exec: PolyglotMap / 2nd exec: HashMap
     * Example now : 1st exec: HashMap / 2nd exec: HashMap
     * The PolyglotMap is transformed to a PolyglotMap before being stored in the cache or being transferred to another node
     * @param o The object to fix the type
     * @return The fixed object
     */
    public static Object fixObject(@Nonnull Object o) {
        Objects.requireNonNull(o);
        if (o instanceof Collection<?> collection) {
            return collection
                .stream()
                .map(WorkflowTypes::fixObject)
                .collect(Collectors.toCollection(LinkedList::new));
        }
        if (o instanceof Map<?, ?> map) {
            return map
                .entrySet()
                .stream()
                .map((e) ->
                    new AbstractMap.SimpleEntry<>(
                        fixObject(e.getKey()),
                        fixObject(e.getValue())
                    )
                )
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        }
        return o;
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
        if (o instanceof FileWrapper) {
            return WFile.of();
        }
        return WObject.of();
    }

    public static WType typeFromString(@Nonnull String strType) {
        Objects.requireNonNull(strType);
        return typeFromString(new StringBuilder(strType));
    }

    private static boolean consumeIfPresent(@Nonnull StringBuilder builder, @Nonnull String toFind) {
        if (builder.toString().startsWith(toFind)) {
            builder.delete(0, toFind.length());
            return true;
        }
        return false;
    }

    private static WType typeFromString(@Nonnull StringBuilder builder) {
        Objects.requireNonNull(builder);
        if (consumeIfPresent(builder, "Map ")) {
            var keyType = (WIterableType) typeFromString(builder);
            if (!consumeIfPresent(builder, " ")) {
                throw new RuntimeException("Type not valid !");
            }
            var valueType = (WIterableType) typeFromString(builder);
            return WMap.of(keyType, valueType);
        }
        if (consumeIfPresent(builder, "Collection ")) {
            var valueType = (WIterableType) typeFromString(builder);
            return WCollection.of(valueType);
        }
        if (consumeIfPresent(builder, "Primitive ")) {
            if (consumeIfPresent(builder, "Integer")) {
                return WPrimitive.Integer;
            }
            if (consumeIfPresent(builder, "String")) {
                return WPrimitive.String;
            }
            if (consumeIfPresent(builder, "Boolean")) {
                return WPrimitive.Boolean;
            }
            if (consumeIfPresent(builder, "Byte")) {
                return WPrimitive.Byte;
            }
            if (consumeIfPresent(builder, "Short")) {
                return WPrimitive.Short;
            }
            if (consumeIfPresent(builder, "Long")) {
                return WPrimitive.Long;
            }
            if (consumeIfPresent(builder, "Float")) {
                return WPrimitive.Float;
            }
            if (consumeIfPresent(builder, "Double")) {
                return WPrimitive.Double;
            }
            if (consumeIfPresent(builder, "Character")) {
                return WPrimitive.Character;
            }
            throw new RuntimeException("Primitive type not supported !");
        }
        if (consumeIfPresent(builder, "Flow")) {
            return WFlow.of();
        }
        if (consumeIfPresent(builder, "Object")) {
            return WObject.of();
        }
        if (consumeIfPresent(builder, "File")) {
            return WFile.of();
        }
        throw new RuntimeException("Type not supported !");
    }

    public static String typeToString(@Nonnull WType type) {
        Objects.requireNonNull(type);
        if (type instanceof WMap map) {
            return "Map %s %s".formatted(typeToString(map.getKeyType()), typeToString(map.getValueType()));
        }
        if (type instanceof WCollection collection) {
            return "Collection %s".formatted(typeToString(collection.getValueType()));
        }
        if (type instanceof WPrimitive primitive) {
            return "Primitive %s".formatted(primitive.name());
        }
        if (type instanceof WFlow) {
            return "Flow";
        }
        if (type instanceof WObject) {
            return "Object";
        }
        if (type instanceof WFile) {
            return "File";
        }
        throw new RuntimeException("Unsupported type !");
    }
}
