package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.connectors.Connector;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class UnmodifiableConnector extends WorkflowNodeError {
    private final Connector connector;
    public UnmodifiableConnector(@Nonnull Connector connector) {
        super(Objects.requireNonNull(connector).getParent());
        this.connector = connector;
    }

    public Connector getConnector() {
        return connector;
    }
}
