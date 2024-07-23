package com.heig.entities.workflow.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.Function;

/**
 * Type representing a primitive
 */
public enum WPrimitive implements WIterableType {
    Integer(0, JsonElement::getAsInt),
    String("", JsonElement::getAsString),
    Boolean(true, JsonElement::getAsBoolean),
    Byte((byte) 0, JsonElement::getAsByte),
    Short((short) 0, JsonElement::getAsShort),
    Long((long) 0, JsonElement::getAsLong),
    Float(0.0f, JsonElement::getAsFloat),
    Double(0.0, JsonElement::getAsDouble),
    Character((char) 0, j -> {
        var str = j.getAsString();
        if (str.length() != 1) {
            throw new IllegalArgumentException("Character must be a single character");
        }
        return str.charAt(0);
    });

    /**
     * The default value for the primitive type
     */
    private final Object defaultValue;

    /**
     * Function allowing to get an object of the correct type from a json element
     */
    private final Function<JsonElement, Object> converter;
    WPrimitive(@Nonnull Object defaultValue, @Nonnull Function<JsonElement, Object> converter) {
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.converter = Objects.requireNonNull(converter);
    }

    @Override
    public boolean canBeConvertedFrom(@Nonnull WType other) {
        Objects.requireNonNull(other);
        if (other instanceof WPrimitive primitive) {
            return this == primitive;
        }
        return false;
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public Object defaultValue() {
        return defaultValue;
    }

    public Object fromJsonElement(@Nonnull JsonElement value) {
        Objects.requireNonNull(value);
        return converter.apply(value);
    }

    public JsonElement toJsonElement(@Nonnull Object value) {
        Objects.requireNonNull(value);
        if (value instanceof Number n) {
            return new JsonPrimitive(n);
        }
        if (value instanceof String s) {
            return new JsonPrimitive(s);
        }
        if (value instanceof Character c) {
            return new JsonPrimitive(c);
        }
        if (value instanceof Boolean b) {
            return new JsonPrimitive(b);
        }
        throw new RuntimeException("Cannot convert " + value + " to Number, String, Character or Boolean");
    }
}
