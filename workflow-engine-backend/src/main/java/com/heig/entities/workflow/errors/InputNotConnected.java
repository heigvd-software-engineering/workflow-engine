package com.heig.entities.workflow.errors;

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
}
