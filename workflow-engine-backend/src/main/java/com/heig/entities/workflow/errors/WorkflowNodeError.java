package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public abstract class WorkflowNodeError extends WorkflowError {
    private final Node node;
    public WorkflowNodeError(@Nonnull Node node) {
        Objects.requireNonNull(node);
        this.node = node;
    }

    public Node getNode() {
        return node;
    }
}
