package com.heig.entities.workflow.errors;

import com.google.gson.JsonObject;

public class NotConnectedGraph extends WorkflowError {
    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        addErrorMessage(obj, "The graph is not connected (meaning it is split in multiple parts that have no links between them)");
        return obj;
    }
}
