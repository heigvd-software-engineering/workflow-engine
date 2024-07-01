package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;

public class CycleDetected extends WorkflowError {
    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        addErrorMessage(obj, "A cycle has been detected");
        return obj;
    }
}
