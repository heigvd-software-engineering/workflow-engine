package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.nodes.Node;
import com.heig.helpers.ResultOrError;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NodeState {
    private State state = State.IDLE;
    private final ConcurrentMap<Integer, ResultOrError<Object>> valuesMap = new ConcurrentHashMap<>();
    private final Node node;
    private boolean hasBeenModified = false;

    public NodeState(@Nonnull Node node) {
        this.node = Objects.requireNonNull(node);
    }

    public void setInputValue(int connectorId, @Nonnull ResultOrError<Object> value) {
        Objects.requireNonNull(value);
        valuesMap.put(connectorId, value);
    }

    public ResultOrError<Object> getInputValue(int connectorId) {
        return valuesMap.get(connectorId);
    }

    public Map<Integer, ResultOrError<Object>> getValues() {
        return Collections.unmodifiableMap(valuesMap);
    }

    public Node getNode() {
        return node;
    }

    public boolean isReady() {
        //For the node to be ready to be executed, we need to have all inputs (except the ones marked as optional) to be available
        return node.getInputs().values().stream().filter(i -> !i.isOptional()).noneMatch(c -> valuesMap.get(c.getId()) == null);
    }

    public synchronized void setState(@Nonnull State state) {
        this.state = Objects.requireNonNull(state);
    }

    public synchronized State getState() {
        return state;
    }

    public void setHasBeenModified(boolean hasBeenModified) {
        this.hasBeenModified = hasBeenModified;
    }

    public boolean hasBeenModified() {
        return hasBeenModified;
    }
}
