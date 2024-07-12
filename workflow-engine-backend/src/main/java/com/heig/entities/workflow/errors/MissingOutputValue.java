package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.connectors.OutputConnector;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class MissingOutputValue extends WorkflowNodeError {
    private final OutputConnector outputConnector;

    public MissingOutputValue(@Nonnull OutputConnector outputConnector) {
        super(Objects.requireNonNull(outputConnector).getParent());
        this.outputConnector = outputConnector;
    }

    public OutputConnector getOutputConnector() {
        return outputConnector;
    }

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        addOutputConnector(obj, outputConnector);
        return obj;
    }

    @Override
    public String toString() {
        return "The value for this connector was not found (check that all the outputs of the node have been set to a value in the code)";
    }
}
