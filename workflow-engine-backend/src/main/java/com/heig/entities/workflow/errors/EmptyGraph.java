package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;

public class EmptyGraph extends WorkflowError {
    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        addErrorMessage(obj, "The graph is empty (no nodes)");
        return obj;
    }
}
