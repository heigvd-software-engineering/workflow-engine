package com.heig.entities.workflow.nodes;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.Connector;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.Function;

public abstract class ModifiableNode extends Node {
    protected ModifiableNode(int id, @Nonnull Workflow workflow) {
        super(id, workflow, false);
    }

    @Override
    public void setTimeout(int timeout) {
        super.setTimeout(timeout);
    }

    @Override
    public void setDeterministic(boolean deterministic) {
        super.setDeterministic(deterministic);
    }

    @Override
    public InputConnector addInputConnector(@Nonnull Function<Integer, InputConnector> connectorSupplier) {
        Objects.requireNonNull(connectorSupplier);
        return super.addInputConnector(connectorSupplier);
    }

    @Override
    public boolean removeInput(@Nonnull InputConnector input) {
        Objects.requireNonNull(input);
        return super.removeInput(input);
    }

    @Override
    public OutputConnector addOutputConnector(@Nonnull Function<Integer, OutputConnector> connectorSupplier) {
        Objects.requireNonNull(connectorSupplier);
        return super.addOutputConnector(connectorSupplier);
    }

    @Override
    public boolean removeOutput(@Nonnull OutputConnector output) {
        Objects.requireNonNull(output);
        return super.removeOutput(output);
    }

    @Override
    public Connector.Builder getConnectorBuilder() {
        return super.getConnectorBuilder();
    }
}
