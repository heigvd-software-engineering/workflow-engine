package com.heig.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import io.smallrye.mutiny.tuples.Tuple4;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Utils {
    private Utils() {}

    public record NodeConnector(int nodeId, int connectorId) { }
    public record Connexion(NodeConnector input, NodeConnector output) { }
    public record Connexions(List<Connexion> connexions) { }

    public static <T> JsonArray serializeList(CustomJsonSerializer<T> elementSerializer, List<T> lstT) {
        var arr = new JsonArray();
        for (var elem : lstT) {
            arr.add(elementSerializer.serialize(elem));
        }
        return arr;
    }

    public static <T> List<T> deserializeList(CustomJsonDeserializer<T> elementDeserializer, JsonArray elementsArr) throws JsonParseException {
        var lstT = new LinkedList<T>();
        for (var elem : elementsArr) {
            lstT.add(elementDeserializer.deserialize(elem));
        }
        return lstT;
    }

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
}
