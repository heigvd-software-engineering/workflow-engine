package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
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

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        addErrorMessage(obj, "Failed execution: %s".formatted(reason));
        return obj;
    }
}
