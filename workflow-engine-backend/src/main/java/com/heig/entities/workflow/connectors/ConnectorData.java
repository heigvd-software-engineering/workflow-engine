package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.errors.NameAlreadyUsed;
import com.heig.entities.workflow.errors.UnmodifiableConnector;
import com.heig.entities.workflow.errors.WorkflowError;
import com.heig.entities.workflow.errors.WorkflowErrors;
import com.heig.entities.workflow.types.WObject;
import com.heig.entities.workflow.types.WType;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;

class ConnectorData {
    protected String name;
    protected WType type;
    protected final Connector connector;

    ConnectorData(@Nonnull Connector connector, @Nonnull String name, @Nonnull WType type) {
        this.connector = Objects.requireNonNull(connector);
        this.type = Objects.requireNonNull(type);
        var optError = setNameIfNotExists(name);
        optError.ifPresent(error -> {
            throw new IllegalArgumentException(error.toString());
        });
    }

    public Optional<WorkflowError> setName(@Nonnull String name) {
        return Optional.of(new UnmodifiableConnector(connector));
    }

    public Optional<WorkflowError> setType(@Nonnull WType type) {
        return Optional.of(new UnmodifiableConnector(connector));
    }

    public String getName() {
        return name;
    }

    public WType getType() {
        return type;
    }

    protected Optional<WorkflowError> setNameIfNotExists(@Nonnull String name) {
        Objects.requireNonNull(name);
        if (connector.getExistingConnectors().anyMatch(i -> Objects.equals(i.getName(), name))) {
            return Optional.of(new NameAlreadyUsed(connector, name));
        }
        this.name = name;
        return Optional.empty();
    }
}
