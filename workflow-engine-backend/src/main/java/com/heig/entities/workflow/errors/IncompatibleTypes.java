package com.heig.entities.workflow.errors;

import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
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
}
