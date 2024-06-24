package com.heig.entities.workflow.types;

import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.Objects;

public interface WType {
    boolean canBeConvertedFrom(@Nonnull WType other);
    Object defaultValue();
    default void toFile(@Nonnull File output, @Nonnull Object value) {
        Objects.requireNonNull(output);
        Objects.requireNonNull(value);
        try (var oos = new ObjectOutputStream(new FileOutputStream(output))) {
            oos.writeObject(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    default Object fromFile(@Nonnull File input) {
        Objects.requireNonNull(input);
        try (var ois = new ObjectInputStream(new FileInputStream(input))) {
            return ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
