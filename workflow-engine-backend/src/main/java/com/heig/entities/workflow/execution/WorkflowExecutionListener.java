package com.heig.entities.workflow.execution;

import jakarta.annotation.Nonnull;

/**
 * Interface used to notify that something in the {@link WorkflowExecutor} has changed
 */
public interface WorkflowExecutionListener {
    /**
     * Called when the {@link State} of the workflow changed
     * @param we The {@link WorkflowExecutor}
     */
    void workflowStateChanged(@Nonnull WorkflowExecutor we);

    /**
     * Called when the {@link NodeState} changed
     * @param state The {@link NodeState}
     */
    void nodeStateChanged(@Nonnull NodeState state);

    /**
     * Called when a new line should be appended to the logs
     * @param line The line to append
     */
    void newLogLine(@Nonnull String line);

    /**
     * Called when a clear of the log has been requested
     */
    void clearLog();
}
