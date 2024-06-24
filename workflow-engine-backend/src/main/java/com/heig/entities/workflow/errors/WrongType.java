package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.types.WType;
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
}
