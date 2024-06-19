package com.heig.entities.workflowErrors;

import com.heig.entities.Node;

public abstract class WorkflowNodeError extends WorkflowError {
    private final Node node;
    public WorkflowNodeError(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }
}
