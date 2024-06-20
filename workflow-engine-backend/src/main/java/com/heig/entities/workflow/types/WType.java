package com.heig.entities.workflow.types;

public interface WType {
    boolean canBeConvertedFrom(WType other);
    Object defaultValue();
}
