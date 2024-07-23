package com.heig.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * The utils class
 */
public class Utils {
    private Utils() {}

    public record NodeConnector(int nodeId, int connectorId) { }
    public record Connexion(NodeConnector input, NodeConnector output) { }
    public record Connexions(List<Connexion> connexions) { }

    /**
     * Serializes a list to json
     * @param elementSerializer The serializer
     * @param lstT The list
     * @return The json
     * @param <T> The type of the list elements
     */
    public static <T> JsonArray serializeList(CustomJsonSerializer<T> elementSerializer, List<T> lstT) {
        var arr = new JsonArray();
        for (var elem : lstT) {
            arr.add(elementSerializer.serialize(elem));
        }
        return arr;
    }

    /**
     * Deserializes a list from json
     * @param elementDeserializer The deserializer
     * @param elementsArr The json
     * @return The list of elements
     * @param <T> The type of the list elements
     * @throws JsonParseException Parsing error
     */
    public static <T> List<T> deserializeList(CustomJsonDeserializer<T> elementDeserializer, JsonArray elementsArr) throws JsonParseException {
        var lstT = new LinkedList<T>();
        for (var elem : elementsArr) {
            lstT.add(elementDeserializer.deserialize(elem));
        }
        return lstT;
    }

    /**
     * Deletes a directory along with all the files and directory inside
     * @param directory The file representing a directory to delete
     */
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
