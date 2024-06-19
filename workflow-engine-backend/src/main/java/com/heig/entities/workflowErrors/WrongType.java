package com.heig.entities.workflowErrors;

import com.heig.entities.Node;
import com.heig.entities.OutputConnector;
import com.heig.entities.workflowTypes.WType;

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
