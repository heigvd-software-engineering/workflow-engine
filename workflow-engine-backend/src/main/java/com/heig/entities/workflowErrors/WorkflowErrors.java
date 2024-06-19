package com.heig.entities.workflowErrors;

import io.vertx.core.impl.ConcurrentHashSet;

import java.util.Collections;
import java.util.Set;

public class WorkflowErrors {
    private final ConcurrentHashSet<WorkflowError> errors = new ConcurrentHashSet<>();

    public void merge(WorkflowErrors other) {
        errors.addAll(other.errors);
    }

    public void clear() {
        errors.clear();
    }

    public void addError(WorkflowError error) {
        errors.add(error);
    }

    public Set<WorkflowError> getErrors() {
        return Collections.unmodifiableSet(errors);
    }
}
