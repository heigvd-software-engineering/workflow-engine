package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

public class ExecutionTimeout extends WorkflowNodeError {
    public ExecutionTimeout(@Nonnull Node node) {
        super(node);
    }

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        addErrorMessage(obj, "Execution timeout (%sms exceeded)".formatted(getNode().getTimeout()));
        return obj;
    }
}
