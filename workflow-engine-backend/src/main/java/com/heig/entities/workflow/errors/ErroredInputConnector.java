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
        addErrorMessage(obj, "The node has an input connector with an error");
        addInputConnector(obj, inputConnector);
        return obj;
    }
}
