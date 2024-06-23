package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.connectors.Connector;

public class UnmodifiableConnector extends WorkflowNodeError {
    private final Connector connector;
    public UnmodifiableConnector(Connector connector) {
        super(connector.getParent());
        this.connector = connector;
    }

    public Connector getConnector() {
        return connector;
    }
}
