package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WFlow;
import jakarta.annotation.Nonnull;

public class OutputFlowConnector extends OutputConnector {
    public static final String CONNECTOR_NAME = "out-flow";
    protected OutputFlowConnector(int id, @Nonnull Node parent) {
        super(id, parent, CONNECTOR_NAME, WFlow.of(), true);
    }

    @Override
    public boolean isOptional() {
        return true;
    }
}
