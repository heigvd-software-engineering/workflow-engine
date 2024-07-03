package com.heig.entities.workflow.connectors;

import com.google.gson.JsonObject;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WType;
import jakarta.annotation.Nonnull;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class InputConnector extends Connector {
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
