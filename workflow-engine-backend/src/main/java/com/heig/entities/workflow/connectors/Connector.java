package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.errors.WorkflowError;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WType;
import io.smallrye.common.annotation.CheckReturnValue;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class Connector {
    public static class Builder {
        private final Node node;
        private final boolean isReadOnly;
        public Builder(@Nonnull Node node, boolean isReadOnly) {
            this.node = Objects.requireNonNull(node);
            this.isReadOnly = isReadOnly;
        }

        public InputConnector buildInputConnector(@Nonnull String name, @Nonnull WType type) {
            return node.addInputConnector((id) -> new InputConnector(id, node, name, type, isReadOnly));
        }

        public OutputConnector buildOutputConnector(@Nonnull String name, @Nonnull WType type) {
            return node.addOutputConnector((id) -> new OutputConnector(id, node, name, type, isReadOnly));
        }
    }

    private final int id;
    private final Node parent;
    private final ConnectorData data;

    public Connector(int id, @Nonnull Node parent, @Nonnull String name, @Nonnull WType type, boolean isReadOnly) {
        if (id < 0) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        this.parent = Objects.requireNonNull(parent);
        this.id = id;
        this.data = isReadOnly ? new ConnectorData(this, name, type) : new ModifiableConnectorData(this, name, type);
    }

    public int getId() {
        return id;
    }

    @Nonnull
    public Node getParent() {
        return parent;
    }

    @Nonnull
    public String getName() {
        return data.getName();
    }

    @CheckReturnValue
    public Optional<WorkflowError> setName(@Nonnull String name) {
        if (Objects.equals(name, this.getName())) {
            return Optional.empty();
        }
        return data.setName(name);
    }

    public WType getType() {
        return data.getType();
    }

    @CheckReturnValue
    public Optional<WorkflowError> setType(@Nonnull WType type) {
        return data.setType(type);
    }

    protected abstract Stream<Connector> getExistingConnectors();
}
