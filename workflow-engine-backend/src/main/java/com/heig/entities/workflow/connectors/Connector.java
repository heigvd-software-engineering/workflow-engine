package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.nodes.ModifiableNode;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WObject;
import com.heig.entities.workflow.types.WType;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.stream.Stream;

public abstract class Connector {
    public static class Builder {
        private final ModifiableNode node;
        public Builder(ModifiableNode node) {
            this.node = node;
        }

        public InputConnector buildInputConnector(String name) {
            return node.addInputConnector((id) -> new InputConnector(id, node, name));
        }

        public OutputConnector buildOutputConnector(String name) {
            return node.addOutputConnector((id) -> new OutputConnector(id, node, name));
        }
    }

    private WType type = WObject.of();
    private String name;
    private final int id;
    private final Node parent;

    public Connector(int id, @Nonnull Node parent, String name) {
        if (id < 0) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(parent);
        this.id = id;
        this.parent = parent;
        setName(name);
    }

    public int getId() {
        return id;
    }

    public Node getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Objects.requireNonNull(name);
        if (getExistingConnectors().anyMatch(i -> Objects.equals(i.getName(), name))) {
            throw new IllegalArgumentException("Connector with the same name already exists");
        }
        this.name = name;
    }

    public WType getType() {
        return type;
    }

    public void setType(@Nonnull WType type) {
        this.type = Objects.requireNonNull(type);
    }

    protected abstract Stream<Connector> getExistingConnectors();
}
