package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

public class ErroredInputConnector extends WorkflowNodeError {
    private final InputConnector inputConnector;
    public ErroredInputConnector(@Nonnull InputConnector inputConnector) {
        super(inputConnector.getParent());
        this.inputConnector = inputConnector;
    }

    public InputConnector getInputConnector() {
        return inputConnector;
    }
}
