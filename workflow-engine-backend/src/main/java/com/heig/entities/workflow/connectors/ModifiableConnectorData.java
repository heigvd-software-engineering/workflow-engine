package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.errors.NameAlreadyUsed;
import com.heig.entities.workflow.errors.WorkflowError;
import com.heig.entities.workflow.types.WType;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;

public class ModifiableConnectorData extends ConnectorData {
    ModifiableConnectorData(@Nonnull Connector connector, @Nonnull String name, @Nonnull WType type) {
        super(connector, name, type);
    }

    @Override
    public Optional<WorkflowError> setName(@Nonnull String name) {
        return setNameIfNotExists(name);
    }

    @Override
    public Optional<WorkflowError> setType(@Nonnull WType type) {
        this.type = Objects.requireNonNull(type);
        return Optional.empty();
    }
}
