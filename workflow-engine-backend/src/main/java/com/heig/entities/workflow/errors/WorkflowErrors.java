package com.heig.entities.workflow.errors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.heig.helpers.CustomJsonDeserializer;
import com.heig.helpers.CustomJsonSerializer;
import com.heig.helpers.Utils;
import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class WorkflowErrors {
    private final ConcurrentHashSet<WorkflowError> errors = new ConcurrentHashSet<>();

    public void merge(@Nonnull WorkflowErrors other) {
        errors.addAll(Objects.requireNonNull(other).errors);
    }

    public void clear() {
        errors.clear();
    }

    public void addError(@Nonnull WorkflowError error) {
        errors.add(Objects.requireNonNull(error));
    }

    public Set<WorkflowError> getErrors() {
        return Collections.unmodifiableSet(errors);
    }
}
