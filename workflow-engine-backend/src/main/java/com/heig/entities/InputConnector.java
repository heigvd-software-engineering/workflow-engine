package com.heig.entities;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;

public class InputConnector extends Connector {
    private OutputConnector connectedTo = null;

    InputConnector(int id, @Nonnull Node parent, String name) {
        super(id, parent, name);
    }

    @Override
    public void setName(@Nonnull String name) {
        Objects.requireNonNull(name);
        if (getParent().getInputs().values().stream().anyMatch(i -> Objects.equals(i.getName(), name))) {
            throw new IllegalArgumentException("Connector with the same name already exists");
        }
        super.setName(name);
    }

    public Optional<OutputConnector> getConnectedTo() {
        return Optional.ofNullable(connectedTo);
    }

    public boolean isOptional() {
        return false;
    }

    void setConnectedTo(OutputConnector connectedTo) {
        this.connectedTo = connectedTo;
    }
}
