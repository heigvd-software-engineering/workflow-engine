package com.heig.entities.workflowTypes;

public class WObject implements WType {
    private static final WObject instance = new WObject();

    private WObject() {}

    public static WObject of() {
        return instance;
    }

    @Override
    public boolean isCompatibleWith(WType other) {
        return true;
    }
}
