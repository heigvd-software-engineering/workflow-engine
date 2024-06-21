package com.heig.entities.workflow.nodes;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.Connector;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.execution.NodeArguments;
import jakarta.annotation.Nonnull;

import java.util.function.Function;

public abstract class ModifiableNode extends Node {
    private final Connector.Builder connectorBuilder = new Connector.Builder(this);

    protected ModifiableNode(int id, @Nonnull Workflow workflow) {
        super(id, workflow);
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
    public InputConnector addInputConnector(Function<Integer, InputConnector> connectorSupplier) {
        return super.addInputConnector(connectorSupplier);
    }

    @Override
    public boolean removeInput(@Nonnull InputConnector input) {
        return super.removeInput(input);
    }

    @Override
    public OutputConnector addOutputConnector(Function<Integer, OutputConnector> connectorSupplier) {
        return super.addOutputConnector(connectorSupplier);
    }

    @Override
    public boolean removeOutput(@Nonnull OutputConnector output) {
        return super.removeOutput(output);
    }

    public Connector.Builder getConnectorBuilder() {
        return connectorBuilder;
    }
}
