package com.heig.entities;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class OutputConnector extends Connector {
    private final List<InputConnector> connectedTo = new LinkedList<>();

    OutputConnector(int id, @Nonnull Node parent, String name) {
        super(id, parent, name);
    }

    @Override
    public void setName(String name) {
        Objects.requireNonNull(name);
        if (getParent().getOutputs().values().stream().anyMatch(i -> Objects.equals(i.getName(), name))) {
            throw new IllegalArgumentException("Connector with the same name already exists");
        }
        super.setName(name);
    }

    public List<InputConnector> getConnectedTo() {
        return Collections.unmodifiableList(connectedTo);
    }

    boolean addConnectedTo(@Nonnull InputConnector connector) {
        Objects.requireNonNull(connector);
        if (connectedTo.contains(connector)) {
            return false;
        }
        return connectedTo.add(connector);
    }

    boolean removeConnectedTo(@Nonnull InputConnector connector) {
        Objects.requireNonNull(connector);
        if (!connectedTo.contains(connector)) {
            return false;
        }
        return connectedTo.remove(connector);
    }
}
