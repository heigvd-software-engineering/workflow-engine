package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class InputConnector extends Connector {
    private OutputConnector connectedTo = null;

    public InputConnector(int id, @Nonnull Node parent, String name) {
        super(id, parent, name);
    }

    public Optional<OutputConnector> getConnectedTo() {
        return Optional.ofNullable(connectedTo);
    }

    public boolean isOptional() {
        return false;
    }

    public void setConnectedTo(OutputConnector connectedTo) {
        this.connectedTo = connectedTo;
    }

    @Override
    protected Stream<Connector> getExistingConnectors() {
        return getParent().getInputs().values().stream().map(Function.identity());
    }
}
