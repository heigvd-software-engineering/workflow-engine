package com.heig.helpers;

import com.google.gson.JsonElement;

public interface CustomJsonSerializer<T> {
    JsonElement serialize(T value);
}
