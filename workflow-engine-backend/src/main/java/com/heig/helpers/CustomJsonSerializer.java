package com.heig.helpers;

import com.google.gson.JsonElement;

/**
 * Custom serialization process
 * @param <T> The type of the serialized value
 */
public interface CustomJsonSerializer<T> {
    /**
     * Serializes a value to json
     * @param value The value
     * @return The json
     */
    JsonElement serialize(T value);
}
