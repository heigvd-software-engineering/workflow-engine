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
    public void setIsDeterministic(boolean deterministic) {
        super.setIsDeterministic(deterministic);
    }

    @Override
    public <T extends InputConnector> T addInputConnector(@Nonnull Function<Integer, T> connectorSupplier) {
        Objects.requireNonNull(connectorSupplier);
        return super.addInputConnector(connectorSupplier);
    }

    @Override
    public boolean removeInput(@Nonnull InputConnector input) {
        Objects.requireNonNull(input);
        if (input.isReadOnly()) {
            return false;
        }
        return super.removeInput(input);
    }

    @Override
    public <T extends OutputConnector> T addOutputConnector(@Nonnull Function<Integer, T> connectorSupplier) {
        Objects.requireNonNull(connectorSupplier);
        return super.addOutputConnector(connectorSupplier);
    }

    @Override
    public boolean removeOutput(@Nonnull OutputConnector output) {
        Objects.requireNonNull(output);
        if (output.isReadOnly()) {
            return false;
        }
        return super.removeOutput(output);
    }

    @Override
    public Connector.Builder getConnectorBuilder() {
        return super.getConnectorBuilder();
    }
}
