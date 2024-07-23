package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.errors.WorkflowError;
import com.heig.entities.workflow.types.WType;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;

/**
 * Stores the connector name and type. Modifiable.
 */
class ModifiableConnectorData extends ConnectorData {
    ModifiableConnectorData(@Nonnull Connector connector, @Nonnull String name, @Nonnull WType type) {
        super(connector, name, type);
    }

    /**
     * Sets the name of the connector
     * @param name The name
     * @return The result of {@link ConnectorData#setNameIfNotExists(String)}
     */
    @Override
    public Optional<WorkflowError> setName(@Nonnull String name) {
        return setNameIfNotExists(name);
    }

    /**
     * Sets the type of the connector
     * @param type The type
     * @return No error
     */
    @Override
    public Optional<WorkflowError> setType(@Nonnull WType type) {
        this.type = Objects.requireNonNull(type);
        return Optional.empty();
    }
}
