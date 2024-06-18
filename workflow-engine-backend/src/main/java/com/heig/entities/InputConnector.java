package com.heig.entities;

import jakarta.annotation.Nonnull;

import java.util.Optional;

public class InputConnector extends Connector {
    private OutputConnector connectedTo = null;

    InputConnector(int id, @Nonnull Node parent) {
        super(id, parent);
    }

    public Optional<OutputConnector> getConnectedTo() {
        return Optional.ofNullable(connectedTo);
    }

    void setConnectedTo(OutputConnector connectedTo) {
        this.connectedTo = connectedTo;
    }
}
