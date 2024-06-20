package com.heig.entities.workflow.types;

public class WObject implements WType {
    private static final WObject instance = new WObject();

    private WObject() {}

    public static WObject of() {
        return instance;
    }

    @Override
    public boolean canBeConvertedFrom(WType other) {
        return true;
    }

    @Override
    public String toString() {
        return "Object";
    }

    @Override
    public Object defaultValue() {
        return new Object();
    }
}
