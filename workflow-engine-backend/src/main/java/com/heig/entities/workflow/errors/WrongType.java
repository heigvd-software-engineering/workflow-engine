package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.types.WType;
import com.heig.entities.workflow.types.WorkflowTypes;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class WrongType extends WorkflowNodeError {
    private final WType actualType;
    private final OutputConnector outputConnector;
    public WrongType(@Nonnull WType actualType, @Nonnull OutputConnector outputConnector) {
        super(Objects.requireNonNull(outputConnector).getParent());
        this.actualType = Objects.requireNonNull(actualType);
        this.outputConnector = outputConnector;
    }

    public WType getActualType() {
        return actualType;
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
        return "The type returned by the node execution (%s) is not compatible with the type defined in the node (%s)".formatted(
            WorkflowTypes.typeToString(actualType),
            WorkflowTypes.typeToString(outputConnector.getType())
        );
    }
}
