package com.heig.entities.workflow.types;

import jakarta.annotation.Nonnull;

public interface WType {
    boolean canBeConvertedFrom(@Nonnull WType other);
    Object defaultValue();
}
