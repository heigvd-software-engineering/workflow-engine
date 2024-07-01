package com.heig.entities.workflow.types;

import com.google.gson.JsonElement;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

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

    private final Object defaultValue;
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
        return converter.apply(value);
    }
}
