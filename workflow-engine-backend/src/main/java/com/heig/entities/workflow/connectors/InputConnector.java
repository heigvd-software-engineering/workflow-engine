package com.heig.entities.workflow.connectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WFlow;
import com.heig.entities.workflow.types.WType;
import com.heig.helpers.Utils;
import jakarta.annotation.Nonnull;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Represents an input connector for a {@link Node}
 */
public class InputConnector extends Connector {
    /**
     * Deserializes an input connector from json
     */
    public static class Deserializer extends ConnectorDeserializer<InputConnector> {
        public Deserializer(Utils.Connexions connexionsToMake, int id, Node parent, String name, WType type, boolean isReadOnly) {
            super(connexionsToMake, id, parent, name, type, isReadOnly);
        }

        @Override
        public InputConnector deserialize(JsonElement value) throws JsonParseException {
            var obj = value.getAsJsonObject();
            InputConnector inputConnector;
            if (type == WFlow.of()) {
                inputConnector = new InputFlowConnector(id, parent, name);
            } else {
                inputConnector = new InputConnector(id, parent, name, type, isReadOnly);
            }
            if (obj.has("connectedTo")) {
                var connectedTo = obj.get("connectedTo").getAsJsonObject();
                var nodeId = connectedTo.get("nodeId").getAsInt();
                var connectorId = connectedTo.get("connectorId").getAsInt();

                connexionsToMake.connexions().add(new Utils.Connexion(new Utils.NodeConnector(parent.getId(), id), new Utils.NodeConnector(nodeId, connectorId)));
            }
            return inputConnector;
        }
    }

    /**
     * The output that this input is connected to
     */
    private OutputConnector connectedTo = null;

    protected InputConnector(int id, @Nonnull Node parent, @Nonnull String name, @Nonnull WType type, boolean isReadOnly) {
        super(id, parent, name, type, isReadOnly);
    }

    public Optional<OutputConnector> getConnectedTo() {
        return Optional.ofNullable(connectedTo);
    }

    /**
     * Changes the output that this input is connected to
     * Notifies the node that a modification has been made with {@link Node#connectorModified(Connector)}
     * @param connectedTo The new output connected (can be null if no output is connected)
     */
    public void setConnectedTo(OutputConnector connectedTo) {
        if (this.connectedTo != connectedTo) {
            this.connectedTo = connectedTo;
            getParent().connectorModified(this);
        }
    }

    /**
     * Return all input connectors of the parent node
     * @return All input connectors of the parent node
     */
    @Override
    protected Stream<Connector> getExistingConnectors() {
        return getParent().getInputs().values().stream().map(Function.identity());
    }

    @Override
    public String toString() {
        return super.toString() + " (I)";
    }

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        if (connectedTo != null) {
            var cto = new JsonObject();
            cto.addProperty("nodeId", connectedTo.getParent().getId());
            cto.addProperty("connectorId", connectedTo.getId());
            obj.add("connectedTo", cto);
        }
        return obj;
    }
}
