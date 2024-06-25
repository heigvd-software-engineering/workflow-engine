package com.heig.helpers;

import jakarta.annotation.Nonnull;

import java.io.File;
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
}
