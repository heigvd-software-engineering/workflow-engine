package com.heig.entities.workflow.connectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.heig.entities.workflow.errors.WorkflowError;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WFlow;
import com.heig.entities.workflow.types.WType;
import com.heig.entities.workflow.types.WorkflowTypes;
import com.heig.helpers.CustomJsonDeserializer;
import com.heig.helpers.Utils;
import io.smallrye.common.annotation.CheckReturnValue;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class Connector {
    public static abstract class ConnectorDeserializer<T> implements CustomJsonDeserializer<T> {
        protected int id;
        protected Node parent;
        protected String name;
        protected WType type;
        protected boolean isReadOnly;
        protected Utils.Connexions connexionsToMake;
        public ConnectorDeserializer(Utils.Connexions connexionsToMake, int id, Node parent, String name, WType type, boolean isReadOnly) {
            this.connexionsToMake = connexionsToMake;
            this.id = id;
            this.parent = parent;
            this.name = name;
            this.type = type;
            this.isReadOnly = isReadOnly;
        }
    }

    public static class Deserializer implements CustomJsonDeserializer<Connector> {
        private final Node parent;
        private final boolean isInputConnector;
        private final Utils.Connexions connexionsToMake;
        public Deserializer(Utils.Connexions connexionsToMake, Node parent, boolean isInputConnector) {
            this.parent = parent;
            this.isInputConnector = isInputConnector;
            this.connexionsToMake = connexionsToMake;
        }

        @Override
        public Connector deserialize(JsonElement value) throws JsonParseException {
            var obj = value.getAsJsonObject();
            var id = obj.get("id").getAsInt();
            var name = obj.get("name").getAsString();
            var type = WorkflowTypes.typeFromString(obj.get("type").getAsString());
            var isReadOnly = obj.get("isReadOnly").getAsBoolean();

            ConnectorDeserializer<? extends Connector> deserializer;
            if (isInputConnector) {
                deserializer = new InputConnector.Deserializer(connexionsToMake, id, parent, name, type, isReadOnly);
            } else {
                deserializer = new OutputConnector.Deserializer(connexionsToMake, id, parent, name, type, isReadOnly);
            }

            return deserializer.deserialize(value);
        }
    }

    public static class Builder {
        private final Node node;
        private final boolean isReadOnly;
        public Builder(@Nonnull Node node, boolean isReadOnly) {
            this.node = Objects.requireNonNull(node);
            this.isReadOnly = isReadOnly;
        }

        public InputConnector buildInputConnector(@Nonnull String name, @Nonnull WType type) {
            if (type == WFlow.of()) {
                return buildInputFlowConnector(name);
            }
            return node.addInputConnector((id) -> new InputConnector(id, node, name, type, isReadOnly));
        }

        public OutputConnector buildOutputConnector(@Nonnull String name, @Nonnull WType type) {
            if (type == WFlow.of()) {
                return buildOutputFlowConnector(name);
            }
            return node.addOutputConnector((id) -> new OutputConnector(id, node, name, type, isReadOnly));
        }

        public InputFlowConnector buildInputFlowConnector(@Nonnull String name) {
            return node.addInputConnector((id) -> new InputFlowConnector(id, node, name));
        }

        public OutputFlowConnector buildOutputFlowConnector(@Nonnull String name) {
            return node.addOutputConnector((id) -> new OutputFlowConnector(id, node, name));
        }
    }

    private final int id;
    private final Node parent;
    private final ConnectorData data;
    private final boolean isReadOnly;

    protected Connector(int id, @Nonnull Node parent, @Nonnull String name, @Nonnull WType type, boolean isReadOnly) {
        if (id < 0) {
            throw new IllegalArgumentException("The id cannot be negative");
        }
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        this.parent = Objects.requireNonNull(parent);
        this.id = id;
        this.isReadOnly = isReadOnly;
        this.data = isReadOnly ? new ConnectorData(this, name, type) : new ModifiableConnectorData(this, name, type);
    }

    public boolean isReadOnly() {
        return isReadOnly;
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
        var optSetName = data.setName(name);
        if (optSetName.isEmpty()) {
            parent.connectorModified(this);
        }
        return optSetName;
    }

    public WType getType() {
        return data.getType();
    }

    @CheckReturnValue
    public Optional<WorkflowError> setType(@Nonnull WType type) {
        var optSetType = data.setType(type);
        if (optSetType.isEmpty()) {
            parent.connectorModified(this);
        }
        return optSetType;
    }

    public boolean isOptional() {
        return false;
    }

    protected abstract Stream<Connector> getExistingConnectors();

    @Override
    public String toString() {
        return getParent() + ": " + getName();
    }

    public JsonObject toJson() {
        var obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("name", data.getName());
        obj.addProperty("type", WorkflowTypes.typeToString(data.getType()));
        obj.addProperty("isReadOnly", isReadOnly);
        obj.addProperty("isOptional", isOptional());
        return obj;
    }
}
