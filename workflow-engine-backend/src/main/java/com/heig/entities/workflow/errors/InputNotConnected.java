package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.connectors.InputConnector;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class InputNotConnected extends WorkflowNodeError {
    private final InputConnector inputConnector;
    public InputNotConnected(@Nonnull InputConnector inputConnector) {
        super(Objects.requireNonNull(inputConnector).getParent());
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
        return "The input is not connected to an output";
    }
}
