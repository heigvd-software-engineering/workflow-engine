package com.heig.entities.workflow.types;

import com.heig.entities.workflow.data.Data;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.Objects;
import java.util.Optional;

/**
 * All types are implementing this interface
 */
public interface WType {
    /**
     * Returns true if the type this method is called on can be converted to the type in parameter
     * @param other The type to try to convert to
     * @return True if the type this method is called on can be converted to the type in parameter, false otherwise
     */
    boolean canBeConvertedFrom(@Nonnull WType other);

    /**
     * Returns the default value for a type
     * @return The default value for a type
     */
    Object defaultValue();

    /**
     * The process to store the value to a cache file
     * @param output The output file name
     * @param value The value to store
     */
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

    /**
     * The process to retrieve a value from a cache file
     * @param input The file to retrieve from
     * @return The object retrieved or {@link Optional#empty()} if it fails
     */
    default Optional<Object> fromFile(@Nonnull File input) {
        if (!input.exists()) {
            return Optional.empty();
        }
        return Data.fromFile(input);
    }

    /**
     * Returns the hash code of the value in parameter
     * @param value The value to get hash code from
     * @return The hash code of the value in parameter
     */
    default int getHashCode(@Nonnull Object value) {
        Objects.requireNonNull(value);
        return value.hashCode();
    }
}
