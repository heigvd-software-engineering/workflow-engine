package com.heig.entities.workflow.errors;

public class NotConnectedGraph extends WorkflowError {
    @Override
    public String toString() {
        return "The graph is not connected (meaning it is split in multiple parts that have no links between them)";
    }
}
