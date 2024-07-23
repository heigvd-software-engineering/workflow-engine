package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.Workflow;

/**
 * The states that a {@link Workflow} or a {@link NodeState} uses
 */
public enum State {
    IDLE,
    RUNNING,
    FAILED,
    FINISHED
}
