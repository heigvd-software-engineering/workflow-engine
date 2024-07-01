package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.types.WorkflowTypes;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class IncompatibleTypes extends WorkflowNodeError {
    private final InputConnector inputConnector;
    private final OutputConnector outputConnector;

    public IncompatibleTypes(@Nonnull InputConnector inputConnector, @Nonnull OutputConnector outputConnector) {
        super(Objects.requireNonNull(inputConnector).getParent());
        this.inputConnector = inputConnector;
        this.outputConnector = Objects.requireNonNull(outputConnector);
    }

    public InputConnector getInputConnector() {
        return inputConnector;
    }

    public OutputConnector getOutputConnector() {
        return outputConnector;
    }

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        addErrorMessage(obj, "%s is not compatible with %s".formatted(
            WorkflowTypes.typeToString(inputConnector.getType()),
            WorkflowTypes.typeToString(outputConnector.getType())
        ));
        addInputConnector(obj, inputConnector);
        addOutputConnector(obj, outputConnector);
        return obj;
    }
}
