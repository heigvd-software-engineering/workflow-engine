package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.types.WType;

public class WrongType extends WorkflowNodeError {
    private final WType actualType;
    public WrongType(WType actualType, OutputConnector outputConnector) {
        super(outputConnector.getParent());
        this.actualType = actualType;
    }

    public WType getActualType() {
        return actualType;
    }
}
