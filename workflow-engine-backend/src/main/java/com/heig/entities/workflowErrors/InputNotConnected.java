package com.heig.entities.workflowErrors;

import com.heig.entities.InputConnector;

public class InputNotConnected extends WorkflowNodeError {
    private final InputConnector inputConnector;
    public InputNotConnected(InputConnector inputConnector) {
        super(inputConnector.getParent());
        this.inputConnector = inputConnector;
    }

    public InputConnector getInputConnectorId() {
        return inputConnector;
    }
}
