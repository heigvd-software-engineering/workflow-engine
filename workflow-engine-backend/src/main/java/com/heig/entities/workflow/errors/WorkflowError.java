package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.connectors.Connector;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;

public abstract class WorkflowError {
    public JsonObject toJson() {
        return new JsonObject();
    }

    protected void addErrorMessage(JsonObject obj, String message) {
        obj.addProperty("error", message);
    }

    protected void addInputConnector(JsonObject obj, InputConnector connector) {
        obj.addProperty("inputConnectorId", connector.getId());
    }

    protected void addOutputConnector(JsonObject obj, OutputConnector connector) {
        obj.addProperty("outputConnectorId", connector.getId());
    }

    protected void addConnector(JsonObject obj, Connector connector) {
        if (connector instanceof InputConnector ic) {
            addInputConnector(obj, ic);
        } else if (connector instanceof OutputConnector oc) {
            addOutputConnector(obj, oc);
        }
    }
}
