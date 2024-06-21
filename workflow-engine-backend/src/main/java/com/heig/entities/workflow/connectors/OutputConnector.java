package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class OutputConnector extends Connector {
    private final List<InputConnector> connectedTo = new LinkedList<>();

    protected OutputConnector(int id, @Nonnull Node parent, String name) {
        super(id, parent, name);
    }

    public List<InputConnector> getConnectedTo() {
        return Collections.unmodifiableList(connectedTo);
    }

    public boolean addConnectedTo(@Nonnull InputConnector connector) {
        Objects.requireNonNull(connector);
        if (connectedTo.contains(connector)) {
            return false;
        }
        return connectedTo.add(connector);
    }

    public boolean removeConnectedTo(@Nonnull InputConnector connector) {
        Objects.requireNonNull(connector);
        if (!connectedTo.contains(connector)) {
            return false;
        }
        return connectedTo.remove(connector);
    }

    @Override
    protected Stream<Connector> getExistingConnectors() {
        return getParent().getOutputs().values().stream().map(Function.identity());
    }
}
