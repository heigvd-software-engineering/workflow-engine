package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.connectors.Connector;

public class NameAlreadyUsed extends WorkflowNodeError {
    private final String name;
    private final Connector connector;
    public NameAlreadyUsed(Connector connector, String name) {
        super(connector.getParent());
        this.connector = connector;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Connector getConnector() {
        return connector;
    }
}
