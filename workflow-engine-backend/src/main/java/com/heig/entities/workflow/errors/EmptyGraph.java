package com.heig.entities.workflow.errors;

public class EmptyGraph extends WorkflowError {
    @Override
    public String toString() {
        return "The graph is empty (no nodes)";
    }
}
