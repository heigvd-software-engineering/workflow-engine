package com.heig.entities;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public abstract class Connector {
    private WorkflowTypes type = WorkflowTypes.Object;

    public WorkflowTypes getType() {
        return type;
    }

    public void setType(@Nonnull WorkflowTypes type) {
        this.type = Objects.requireNonNull(type);
    }
}
