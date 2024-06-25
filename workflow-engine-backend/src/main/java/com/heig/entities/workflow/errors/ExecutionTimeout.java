package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

public class ExecutionTimeout extends WorkflowNodeError {
    public ExecutionTimeout(@Nonnull Node node) {
        super(node);
    }
}
