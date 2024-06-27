package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

public interface WorkflowExecutionListener {
    void workflowStateChanged(@Nonnull State state);
    void nodeStateChanged(@Nonnull Node node, @Nonnull State state);
}
