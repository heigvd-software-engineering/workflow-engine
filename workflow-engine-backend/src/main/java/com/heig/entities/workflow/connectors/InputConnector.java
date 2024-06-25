package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WType;
import jakarta.annotation.Nonnull;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class InputConnector extends Connector {
    private OutputConnector connectedTo = null;

    protected InputConnector(int id, @Nonnull Node parent, @Nonnull String name, @Nonnull WType type, boolean isReadOnly) {
        super(id, parent, name, type, isReadOnly);
    }

    public Optional<OutputConnector> getConnectedTo() {
        return Optional.ofNullable(connectedTo);
    }

    public void setConnectedTo(OutputConnector connectedTo) {
        this.connectedTo = connectedTo;
    }

    @Override
    protected Stream<Connector> getExistingConnectors() {
        return getParent().getInputs().values().stream().map(Function.identity());
    }
}
