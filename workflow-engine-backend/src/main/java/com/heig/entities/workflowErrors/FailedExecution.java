package com.heig.entities.workflowErrors;

import com.heig.entities.Node;

public class FailedExecution extends WorkflowNodeError {
    private final String reason;
    public FailedExecution(Node node, String reason) {
        super(node);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
