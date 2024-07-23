package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Represents an error that happened in a {@link Node}
 */
public abstract class WorkflowNodeError extends WorkflowError {
    private final Node node;
    public WorkflowNodeError(@Nonnull Node node) {
        this.node = Objects.requireNonNull(node);
    }

    public Node getNode() {
        return node;
    }

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        obj.addProperty("type", "node");
        obj.addProperty("nodeId", node.getId());
        return obj;
    }
}
