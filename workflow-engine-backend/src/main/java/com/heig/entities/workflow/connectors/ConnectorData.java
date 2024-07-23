package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.errors.NameAlreadyUsed;
import com.heig.entities.workflow.errors.UnmodifiableConnector;
import com.heig.entities.workflow.errors.WorkflowError;
import com.heig.entities.workflow.types.WType;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;

/**
 * Stores the connector name and type. Read only.
 */
class ConnectorData {
    /**
     * The name of the connector
     */
    protected String name;

    /**
     * The type of the connector
     */
    protected WType type;

    /**
     * The connector linked to this {@link ConnectorData} instance
     */
    protected final Connector connector;

    ConnectorData(@Nonnull Connector connector, @Nonnull String name, @Nonnull WType type) {
        this.connector = Objects.requireNonNull(connector);
        this.type = Objects.requireNonNull(type);
        //Tries to set the name of the connector and throws an error if a connector with the same name already exists
        var optError = setNameIfNotExists(name);
        optError.ifPresent(error -> {
            throw new IllegalArgumentException(error.toString());
        });
    }

    /**
     * Returns an error because the {@link ConnectorData} class cannot be used to change the name
     * @param name The name
     * @return An error
     */
    public Optional<WorkflowError> setName(@Nonnull String name) {
        return Optional.of(new UnmodifiableConnector(connector));
    }

    /**
     * Returns an error because the {@link ConnectorData} class cannot be used to change the type
     * @param type The type
     * @return An error
     */
    public Optional<WorkflowError> setType(@Nonnull WType type) {
        return Optional.of(new UnmodifiableConnector(connector));
    }

    public String getName() {
        return name;
    }

    public WType getType() {
        return type;
    }

    /**
     * Sets the name of the connector if no connector with the same name already exists
     * @param name The name to use
     * @return An error if another connector with the same name exists, no error otherwise
     */
    protected Optional<WorkflowError> setNameIfNotExists(@Nonnull String name) {
        Objects.requireNonNull(name);
        if (connector.getExistingConnectors().anyMatch(i -> Objects.equals(i.getName(), name))) {
            return Optional.of(new NameAlreadyUsed(connector, name));
        }
        this.name = name;
        return Optional.empty();
    }
}
