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

public class InputConnector extends Connector {
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

    private OutputConnector connectedTo = null;

    protected InputConnector(int id, @Nonnull Node parent, @Nonnull String name, @Nonnull WType type, boolean isReadOnly) {
        super(id, parent, name, type, isReadOnly);
    }

    public Optional<OutputConnector> getConnectedTo() {
        return Optional.ofNullable(connectedTo);
    }

    public void setConnectedTo(OutputConnector connectedTo) {
        if (this.connectedTo != connectedTo) {
            this.connectedTo = connectedTo;
            getParent().connectorModified(this);
        }
    }

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
