package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.connectors.Connector;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;

/**
 * Represents a workflow error
 */
public abstract class WorkflowError {
    /**
     * The json representation of the error
     * @return The json representation of the error
     */
    public JsonObject toJson() {
        var obj = new JsonObject();
        obj.addProperty("error", toString());
        obj.addProperty("errorType", getClass().getSimpleName());
        obj.addProperty("type", "general");
        return obj;
    }

    /**
     * Adds an input connector to the error json representation
     * @param obj The json object
     * @param connector The input
     */
    protected void addInputConnector(JsonObject obj, InputConnector connector) {
        obj.addProperty("connectorType", "input");
        obj.addProperty("connectorId", connector.getId());
    }

    /**
     * Adds an output connector to the error json representation
     * @param obj The json object
     * @param connector The output
     */
    protected void addOutputConnector(JsonObject obj, OutputConnector connector) {
        obj.addProperty("connectorType", "output");
        obj.addProperty("connectorId", connector.getId());
    }

    /**
     * Adds either an input or output connector to the error json representation by using {@link WorkflowError#addInputConnector(JsonObject, InputConnector)} or {@link WorkflowError#addOutputConnector(JsonObject, OutputConnector)}
     * @param obj The json object
     * @param connector The connector
     */
    protected void addConnector(JsonObject obj, Connector connector) {
        if (connector instanceof InputConnector ic) {
            addInputConnector(obj, ic);
        } else if (connector instanceof OutputConnector oc) {
            addOutputConnector(obj, oc);
        }
    }
}
