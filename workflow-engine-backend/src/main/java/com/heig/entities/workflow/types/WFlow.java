package com.heig.entities.workflow.types;

import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

public class WFlow implements WType {
    private static final WFlow instance = new WFlow();

    private WFlow() { }

    public static WFlow of() {
        return instance;
    }

    @Override
    public boolean canBeConvertedFrom(@Nonnull WType other) {
        Objects.requireNonNull(other);
        return other instanceof WFlow;
    }

    @Override
    public Object defaultValue() {
        return instance;
    }

    @Override
    public String toString() {
        return "Flow";
    }

    @Override
    public void toFile(@Nonnull File output, @Nonnull Object value) {
        //No need to save the input to a file as there are no data to save for a flow connector
    }

    @Override
    public Optional<Object> fromFile(@Nonnull File input) {
        return Optional.of(instance);
    }

    @Override
    public int getHashCode(@Nonnull Object value) {
        //Here value will always be the instance of WFlow, and we need to always have the same hashcode to use for the cache
        //I don't want to modify the hashCode method of WFlow
        return 1;
    }
}
