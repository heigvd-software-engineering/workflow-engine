package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.nodes.Node;

public abstract class WorkflowNodeError extends WorkflowError {
    private final Node node;
    public WorkflowNodeError(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }
}
