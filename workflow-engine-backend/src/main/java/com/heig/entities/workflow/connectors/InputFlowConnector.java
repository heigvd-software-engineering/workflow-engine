package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WFlow;
import jakarta.annotation.Nonnull;

public class InputFlowConnector extends InputConnector {
    protected InputFlowConnector(int id, @Nonnull Node parent, @Nonnull String name) {
        super(id, parent, name, WFlow.of(), false);
    }

    @Override
    public boolean isOptional() {
        return true;
    }
}
