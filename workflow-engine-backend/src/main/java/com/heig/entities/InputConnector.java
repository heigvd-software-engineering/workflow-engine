package com.heig.entities;

import java.util.Optional;

public class InputConnector extends Connector {
    private OutputConnector connectedTo = null;

    public Optional<OutputConnector> getConnectedTo() {
        return Optional.ofNullable(connectedTo);
    }

    void setConnectedTo(OutputConnector connectedTo) {
        this.connectedTo = connectedTo;
    }
}
