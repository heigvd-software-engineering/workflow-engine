package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.connectors.InputConnector;
import jakarta.annotation.Nonnull;

public class ErroredInputConnector extends WorkflowNodeError {
    private final InputConnector inputConnector;
    public ErroredInputConnector(@Nonnull InputConnector inputConnector) {
        super(inputConnector.getParent());
        this.inputConnector = inputConnector;
    }

    public InputConnector getInputConnector() {
        return inputConnector;
    }

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        addInputConnector(obj, inputConnector);
        return obj;
    }

    @Override
    public String toString() {
        return "The input connector is connected to an errored node";
    }
}
