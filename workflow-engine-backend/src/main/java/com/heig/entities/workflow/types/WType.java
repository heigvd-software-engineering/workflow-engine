package com.heig.entities.workflow.types;

import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.Objects;
import java.util.Optional;

public interface WType {
    boolean canBeConvertedFrom(@Nonnull WType other);
    Object defaultValue();
    default void toFile(@Nonnull File output, @Nonnull Object value) {
        WorkflowTypes.toFile(output, value);
    }
    default Optional<Object> fromFile(@Nonnull File input) {
        return WorkflowTypes.fromFile(input);
    }
    default int getHashCode(@Nonnull Object value) {
        Objects.requireNonNull(value);
        return value.hashCode();
    }
}
