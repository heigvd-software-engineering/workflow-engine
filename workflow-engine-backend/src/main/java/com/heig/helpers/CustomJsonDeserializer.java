package com.heig.helpers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Custom deserialization process
 * @param <T> The type of the deserialized value
 */
public interface CustomJsonDeserializer<T> {
    /**
     * Deserializes the json
     * @param value The json
     * @return The value obtained
     * @throws JsonParseException Parsing error
     */
    T deserialize(JsonElement value) throws JsonParseException;
}
