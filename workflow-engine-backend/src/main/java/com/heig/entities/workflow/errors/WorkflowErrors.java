package com.heig.entities.workflow.errors;

import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Represents multiple errors
 */
public class WorkflowErrors {
    /**
     * All the errors
     */
    private final ConcurrentHashSet<WorkflowError> errors = new ConcurrentHashSet<>();

    /**
     * Adds all the errors contained in the {@link WorkflowErrors} in parameter to the current one
     * @param other The errors to add
     */
    public void merge(@Nonnull WorkflowErrors other) {
        errors.addAll(Objects.requireNonNull(other).errors);
    }

    /**
     * Removes all errors
     */
    public void clear() {
        errors.clear();
    }

    /**
     * Add a single error
     * @param error The error to add
     */
    public void addError(@Nonnull WorkflowError error) {
        errors.add(Objects.requireNonNull(error));
    }

    /**
     * Returns a set of errors
     * @return A set of errors
     */
    public Set<WorkflowError> getErrors() {
        return Collections.unmodifiableSet(errors);
    }
}
