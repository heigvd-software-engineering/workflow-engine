package com.heig.entities.workflow.nodes;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.Connector;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.Function;

public abstract class ModifiableNode extends Node {
    public static String IN_FLOW = "in-flow";
    public static String OUT_FLOW = "out-flow";

    protected ModifiableNode(int id, @Nonnull Workflow workflow) {
        super(id, workflow, false);

        //We add the input and output flow connector for every modifiable node
        connectorBuilder.buildInputFlowConnector(IN_FLOW);
        connectorBuilder.buildOutputFlowConnector(OUT_FLOW);
    }

    @Override
    public void setTimeout(int timeout) {
        if (this.getTimeout() != timeout) {
            super.setTimeout(timeout);
            getWorkflow().nodeModified(this);
        }
    }

    @Override
    public void setIsDeterministic(boolean deterministic) {
        if (isDeterministic() != deterministic) {
            super.setIsDeterministic(deterministic);
            getWorkflow().nodeModified(this);
        }
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
