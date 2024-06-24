package com.heig.entities.workflow.errors;

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
}
