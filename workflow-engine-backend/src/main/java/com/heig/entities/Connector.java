package com.heig.entities;

import com.heig.entities.workflowTypes.WObject;
import com.heig.entities.workflowTypes.WType;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public abstract class Connector {
    private WType type = WObject.of();
    private String name;
    private final int id;
    private final Node parent;

    Connector(int id, @Nonnull Node parent, String name) {
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
        this.name = name;
    }

    public WType getType() {
        return type;
    }

    public void setType(@Nonnull WType type) {
        this.type = Objects.requireNonNull(type);
    }
}