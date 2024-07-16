package com.heig.entities.workflow.types;

import com.heig.entities.workflow.data.Data;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.Objects;
import java.util.Optional;

public interface WType {
    boolean canBeConvertedFrom(@Nonnull WType other);
    Object defaultValue();
    default void toFile(@Nonnull File output, @Nonnull Object value) {
        try {
            if (!output.createNewFile()) {
                throw new RuntimeException("Could not create cache file type");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Data.toFile(output, value);
    }
    default Optional<Object> fromFile(@Nonnull File input) {
        if (!input.exists()) {
            return Optional.empty();
        }
        return Data.fromFile(input);
    }
    default int getHashCode(@Nonnull Object value) {
        Objects.requireNonNull(value);
        return value.hashCode();
    }
}
