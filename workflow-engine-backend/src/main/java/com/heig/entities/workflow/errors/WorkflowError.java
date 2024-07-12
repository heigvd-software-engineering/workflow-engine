package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.connectors.Connector;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;

public abstract class WorkflowError {
    public JsonObject toJson() {
        var obj = new JsonObject();
        obj.addProperty("error", toString());
        obj.addProperty("errorType", getClass().getSimpleName());
        obj.addProperty("type", "general");
        return obj;
    }

    protected void addInputConnector(JsonObject obj, InputConnector connector) {
        obj.addProperty("connectorType", "input");
        obj.addProperty("connectorId", connector.getId());
    }

    protected void addOutputConnector(JsonObject obj, OutputConnector connector) {
        obj.addProperty("connectorType", "output");
        obj.addProperty("connectorId", connector.getId());
    }

    protected void addConnector(JsonObject obj, Connector connector) {
        if (connector instanceof InputConnector ic) {
            addInputConnector(obj, ic);
        } else if (connector instanceof OutputConnector oc) {
            addOutputConnector(obj, oc);
        }
    }
}
