package com.heig.helpers;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Objects;

public class Utils {
    private Utils() {}

    public static void deleteCompleteDirectory(@Nonnull File directory) {
        Objects.requireNonNull(directory);
        if (directory.exists()) {
            var rootDirectory = Paths.get(directory.toURI());
            try (var dirStream = Files.walk(rootDirectory)) {
                dirStream
                    .map(Path::toFile)
                    .sorted(Comparator.reverseOrder())
                    .forEach(File::delete);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static JsonReader getJsonReader(@Nonnull String json) {
        Objects.requireNonNull(json);

        var g = new Gson();
        return g.newJsonReader(new StringReader(json));
    }

    public static void readJsonArray(@Nonnull JsonReader jsonReader, @Nonnull ThrowingConsumer<JsonReader, IOException> consumer) throws IOException {
        Objects.requireNonNull(jsonReader);
        Objects.requireNonNull(consumer);

        jsonReader.beginArray();
        consumer.accept(jsonReader);
        jsonReader.endArray();
    }

    public static void readJsonObject(@Nonnull JsonReader jsonReader, @Nonnull ThrowingConsumer<String, IOException> consumer) throws IOException {
        Objects.requireNonNull(jsonReader);
        Objects.requireNonNull(consumer);

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            consumer.accept(jsonReader.nextName());
        }
        jsonReader.endObject();
    }
}
