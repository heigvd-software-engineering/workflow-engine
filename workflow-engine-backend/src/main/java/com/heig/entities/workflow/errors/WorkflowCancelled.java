package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;

public class WorkflowCancelled extends WorkflowError {
    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        addErrorMessage(obj, "The execution of the workflow has been cancelled");
        return obj;
    }
}
