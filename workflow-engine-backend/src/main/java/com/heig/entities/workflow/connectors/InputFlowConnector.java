package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WFlow;
import jakarta.annotation.Nonnull;

public class InputFlowConnector extends InputConnector {
    public static final String CONNECTOR_NAME = "in-flow";
    protected InputFlowConnector(int id, @Nonnull Node parent) {
        super(id, parent, CONNECTOR_NAME, WFlow.of(), true);
    }

    @Override
    public boolean isOptional() {
        return true;
    }
}
