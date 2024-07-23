package com.heig.entities.workflow.connectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.heig.entities.workflow.errors.WorkflowError;
import com.heig.entities.workflow.nodes.ModifiableNode;
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

/**
 * Represent a connector (input / output) of a {@link Node}
 */
public abstract class Connector {
    /**
     * Used to deserialize a specific connector json from a save
     * @param <T> The class implementing {@link Connector}
     */
    public static abstract class ConnectorDeserializer<T extends Connector> implements CustomJsonDeserializer<T> {
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

    /**
     * Common part to deserialize a json for a connector
     */
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

            //Depending on if the connector is an input or output, we will use different deserializer
            ConnectorDeserializer<? extends Connector> deserializer;
            if (isInputConnector) {
                deserializer = new InputConnector.Deserializer(connexionsToMake, id, parent, name, type, isReadOnly);
            } else {
                deserializer = new OutputConnector.Deserializer(connexionsToMake, id, parent, name, type, isReadOnly);
            }

            return deserializer.deserialize(value);
        }
    }

    /**
     * Builder allows to create connectors for a node
     */
    public static class Builder {
        private final Node node;
        private final boolean isReadOnly;
        public Builder(@Nonnull Node node, boolean isReadOnly) {
            this.node = Objects.requireNonNull(node);
            this.isReadOnly = isReadOnly;
        }

        /**
         * Builds an {@link InputConnector}
         * @param name The name of the connector
         * @param type The type of the connector
         * @return The {@link InputConnector}
         */
        public InputConnector buildInputConnector(@Nonnull String name, @Nonnull WType type) {
            if (type == WFlow.of()) {
                return buildInputFlowConnector(name);
            }
            return node.addInputConnector((id) -> new InputConnector(id, node, name, type, isReadOnly));
        }

        /**
         * Builds an {@link OutputConnector}
         * @param name The name of the connector
         * @param type The type of the connector
         * @return The {@link OutputConnector}
         */
        public OutputConnector buildOutputConnector(@Nonnull String name, @Nonnull WType type) {
            if (type == WFlow.of()) {
                return buildOutputFlowConnector(name);
            }
            return node.addOutputConnector((id) -> new OutputConnector(id, node, name, type, isReadOnly));
        }

        /**
         * Builds an {@link InputFlowConnector}
         * @param name The name of the connector
         * @return The {@link InputFlowConnector}
         */
        public InputFlowConnector buildInputFlowConnector(@Nonnull String name) {
            return node.addInputConnector((id) -> new InputFlowConnector(id, node, name));
        }

        /**
         * Builds an {@link OutputFlowConnector}
         * @param name The name of the connector
         * @return The {@link OutputFlowConnector}
         */
        public OutputFlowConnector buildOutputFlowConnector(@Nonnull String name) {
            return node.addOutputConnector((id) -> new OutputFlowConnector(id, node, name));
        }
    }

    /**
     * The id of the connector
     */
    private final int id;

    /**
     * The node where the connector is located in
     */
    private final Node parent;

    /**
     * The data storing the name and the type of the connector
     */
    private final ConnectorData data;

    /**
     * Whether the connector is read only (no name or type changes allowed and no adding or removing connectors by {@link ModifiableNode})
     */
    private final boolean isReadOnly;

    /**
     * Constructs a connector
     * @param id The id
     * @param parent The parent node
     * @param name The name
     * @param type The type
     * @param isReadOnly Whether the connector is read only
     */
    protected Connector(int id, @Nonnull Node parent, @Nonnull String name, @Nonnull WType type, boolean isReadOnly) {
        if (id < 0) {
            throw new IllegalArgumentException("The id cannot be negative");
        }
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        this.parent = Objects.requireNonNull(parent);
        this.id = id;
        this.isReadOnly = isReadOnly;
        //If the connector is read only, the data will be stored in a ModifiableConnectorData otherwise in a ConnectorData
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

    /**
     * Sets the name of the connector. Returns an error if setting the name with {@link ConnectorData#setName(String)} returns one
     * @param name The name
     * @return An optional error
     */
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

    /**
     * Sets the type of the connector. Returns an error if setting the name with {@link ConnectorData#setType(WType)} returns one
     * @param type The type
     * @return An optional error
     */
    @CheckReturnValue
    public Optional<WorkflowError> setType(@Nonnull WType type) {
        var optSetType = data.setType(type);
        if (optSetType.isEmpty()) {
            parent.connectorModified(this);
        }
        return optSetType;
    }

    public boolean isOptional() {
        //By default, a connector is not optional
        return false;
    }

    /**
     * Returns the list of already existing connectors (if the connector is an Input, all the InputConnectors will be returned by this method)
     * @return The list of already existing connectors
     */
    protected abstract Stream<Connector> getExistingConnectors();

    @Override
    public String toString() {
        return getParent() + ": " + getName();
    }

    /**
     * Converts the data of the connector to a json representation
     * @return The data of the connector under a json format
     */
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
