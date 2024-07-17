package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

public interface WorkflowExecutionListener {
    void workflowStateChanged(@Nonnull WorkflowExecutor we);
    void nodeStateChanged(@Nonnull NodeState state);
    void newLogLine(@Nonnull String line);
    void clearLog();
}
