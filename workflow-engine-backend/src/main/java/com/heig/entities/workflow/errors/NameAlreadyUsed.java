package com.heig.entities.workflow.errors;

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
}
