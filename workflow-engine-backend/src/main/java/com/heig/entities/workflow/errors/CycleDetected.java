package com.heig.entities.workflow.errors;

public class CycleDetected extends WorkflowError {
    @Override
    public String toString() {
        return "A cycle has been detected";
    }
}
