package com.heig.entities.workflow.connectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WFlow;
import com.heig.entities.workflow.types.WType;
import com.heig.helpers.Utils;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Represents an output connector for a {@link Node}
 */
public class OutputConnector extends Connector {
    /**
     * Deserializes an output connector from json
     */
    public static class Deserializer extends ConnectorDeserializer<OutputConnector> {
        public Deserializer(Utils.Connexions connexionsToMake, int id, Node parent, String name, WType type, boolean isReadOnly) {
            super(connexionsToMake, id, parent, name, type, isReadOnly);
        }

        @Override
        public OutputConnector deserialize(JsonElement value) throws JsonParseException {
            OutputConnector outputConnector;
            if (type == WFlow.of()) {
                outputConnector = new OutputFlowConnector(id, parent, name);
            } else {
                outputConnector = new OutputConnector(id, parent, name, type, isReadOnly);
            }
            return outputConnector;
        }
    }

    /**
     * List of all the inputs connected to this output
     */
    private final List<InputConnector> connectedTo = new LinkedList<>();

    protected OutputConnector(int id, @Nonnull Node parent, @Nonnull String name, @Nonnull WType type, boolean isReadOnly) {
        super(id, parent, name, type, isReadOnly);
    }

    /**
     * Returns a list of all the inputs connected to this output
     * @return A list of all the inputs connected to this output
     */
    public List<InputConnector> getConnectedTo() {
        return Collections.unmodifiableList(connectedTo);
    }

    /**
     * Adds an input connected to this output
     * @param connector The input to connect to this output
     * @return False if the input is already connected to this output, true otherwise
     */
    public boolean addConnectedTo(@Nonnull InputConnector connector) {
        Objects.requireNonNull(connector);
        if (connectedTo.contains(connector)) {
            return false;
        }
        connectedTo.add(connector);
        getParent().connectorModified(this);
        return true;
    }

    /**
     * Removes an input connected to this output
     * @param connector The input to disconnect from this output
     * @return False if the input is not connected to this output, true otherwise
     */
    public boolean removeConnectedTo(@Nonnull InputConnector connector) {
        Objects.requireNonNull(connector);
        if (!connectedTo.contains(connector)) {
            return false;
        }
        var removeSuccess = connectedTo.remove(connector);
        getParent().connectorModified(this);
        return removeSuccess;
    }

    /**
     * Return all output connectors of the parent node
     * @return All output connectors of the parent node
     */
    @Override
    protected Stream<Connector> getExistingConnectors() {
        return getParent().getOutputs().values().stream().map(Function.identity());
    }

    @Override
    public String toString() {
        return super.toString() + " (O)";
    }

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();

        var connectedTo = new JsonArray();
        for (var connector : getConnectedTo()) {
            var cto = new JsonObject();
            cto.addProperty("nodeId", connector.getParent().getId());
            cto.addProperty("connectorId", connector.getId());
            connectedTo.add(cto);
        }

        obj.add("connectedTo", connectedTo);
        return obj;
    }
}
