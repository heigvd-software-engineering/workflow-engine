package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.connectors.OutputConnector;

public class MissingOutputValue extends WorkflowNodeError {
    private final OutputConnector outputConnector;

    public MissingOutputValue(OutputConnector outputConnector) {
        super(outputConnector.getParent());
        this.outputConnector = outputConnector;
    }

    public OutputConnector getOutputConnector() {
        return outputConnector;
    }
}
