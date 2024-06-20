package com.heig.entities.workflow.execution;

import jakarta.annotation.Nonnull;

import java.util.*;

public class NodeArguments {
    private Map<String, Object> arguments = new HashMap<>();

    public void putArgument(String name, @Nonnull Object value) {
        Objects.requireNonNull(value);
        arguments.put(name, value);
    }

    public Optional<Object> getArgument(String name) {
        return Optional.ofNullable(arguments.get(name));
    }

    public Map<String, Object> getArguments() {
        return Collections.unmodifiableMap(arguments);
    }
}
