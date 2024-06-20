package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.connectors.InputConnector;

public class InputNotConnected extends WorkflowNodeError {
    private final InputConnector inputConnector;
    public InputNotConnected(InputConnector inputConnector) {
        super(inputConnector.getParent());
        this.inputConnector = inputConnector;
    }

    public InputConnector getInputConnector() {
        return inputConnector;
    }
}
