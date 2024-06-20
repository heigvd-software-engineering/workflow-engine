package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;

public class IncompatibleTypes extends WorkflowNodeError {
    private final InputConnector inputConnector;
    private final OutputConnector outputConnector;

    public IncompatibleTypes(InputConnector inputConnector, OutputConnector outputConnector) {
        super(inputConnector.getParent());
        this.inputConnector = inputConnector;
        this.outputConnector = outputConnector;
    }

    public InputConnector getInputConnector() {
        return inputConnector;
    }

    public OutputConnector getOutputConnector() {
        return outputConnector;
    }
}
