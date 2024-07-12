package com.heig.entities.workflow.errors;

public class WorkflowCancelled extends WorkflowError {
    @Override
    public String toString() {
        return "The execution of the workflow has been cancelled";
    }
}
