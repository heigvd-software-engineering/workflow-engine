package com.heig.helpers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public interface CustomJsonDeserializer<T> {
    T deserialize(JsonElement value) throws JsonParseException;
}
