package com.heig.entities.workflow.execution;

import jakarta.annotation.Nonnull;

import java.util.*;

public class NodeArguments {
    private final Map<String, Object> arguments = new HashMap<>();
    public void putArgument(@Nonnull String name, @Nonnull Object value) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(name);
        arguments.put(name, value);
    }

    public Optional<Object> getArgument(@Nonnull String name) {
        Objects.requireNonNull(name);
        return Optional.ofNullable(arguments.get(name));
    }

    public Map<String, Object> getArguments() {
        return Collections.unmodifiableMap(arguments);
    }
}
