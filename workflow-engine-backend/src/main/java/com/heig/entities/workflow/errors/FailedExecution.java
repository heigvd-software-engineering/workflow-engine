package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class FailedExecution extends WorkflowNodeError {
    private final String reason;
    public FailedExecution(@Nonnull Node node, @Nonnull String reason) {
        super(node);
        this.reason = Objects.requireNonNull(reason);
    }

    public String getReason() {
        return reason;
    }
}
