package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.connectors.Connector;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class NameAlreadyUsed extends WorkflowNodeError {
    private final String name;
    private final Connector connector;
    public NameAlreadyUsed(@Nonnull Connector connector, @Nonnull String name) {
        super(Objects.requireNonNull(connector).getParent());
        this.connector = connector;
        this.name = Objects.requireNonNull(name);
    }

    public String getName() {
        return name;
    }

    public Connector getConnector() {
        return connector;
    }

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        addConnector(obj, connector);
        return obj;
    }

    @Override
    public String toString() {
        return "The name '%s' is already used by another connector".formatted(name);
    }
}
