package com.heig.entities.workflow.nodes;

import com.heig.entities.workflow.connectors.Connector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.types.WPrimitive;
import com.heig.entities.workflow.types.WType;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class Node {
    public static class Builder {
        private final Workflow workflow;
        public Builder(@Nonnull Workflow workflow) {
            this.workflow = Objects.requireNonNull(workflow);
        }

        public CodeNode buildCodeNode() {
            return workflow.addNode((id) -> new CodeNode(id, workflow));
        }

        public PrimitiveNode buildPrimitiveNode(@Nonnull WPrimitive type) {
            return workflow.addNode((id) -> new PrimitiveNode(id, workflow, type));
        }
    }

    protected final Connector.Builder connectorBuilder;
    private final AtomicInteger currentId = new AtomicInteger(0);

    private boolean isDeterministic = false;
    private int timeout = 5000;

    private final int id;
    private final Workflow workflow;
    private final ConcurrentMap<Integer, InputConnector> inputs = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, OutputConnector> outputs = new ConcurrentHashMap<>();

    protected Node(int id, @Nonnull Workflow workflow, boolean areConnectorsReadOnly) {
        if (id < 0) {
            throw new IllegalArgumentException("The id cannot be negative");
        }
        this.id = id;
        this.workflow = Objects.requireNonNull(workflow);
        this.connectorBuilder = new Connector.Builder(this, areConnectorsReadOnly);

        //We add the input and output flow connector for every node
        connectorBuilder.buildInputFlowConnector();
        connectorBuilder.buildOutputFlowConnector();
    }

    public boolean isDeterministic() {
        return isDeterministic;
    }

    protected void setIsDeterministic(boolean deterministic) {
        isDeterministic = deterministic;
    }

    public int getTimeout() {
        return timeout;
    }

    protected void setTimeout(int timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("The timeout must be greater than 0");
        }
        this.timeout = timeout;
    }

    public int getId() {
        return id;
    }

    public Map<Integer, InputConnector> getInputs() {
        return Collections.unmodifiableMap(inputs);
    }

    public Optional<InputConnector> getInput(int id) {
        return Optional.ofNullable(inputs.get(id));
    }

    public Map<Integer, OutputConnector> getOutputs() {
        return Collections.unmodifiableMap(outputs);
    }

    public Optional<OutputConnector> getOutput(int id) {
        return Optional.ofNullable(outputs.get(id));
    }

    public void disconnectEverything() {
        inputs.values().forEach(workflow::disconnect);
        outputs.values().forEach(output -> output.getConnectedTo().forEach(workflow::disconnect));
        workflow.nodeModified(this);
    }

    protected boolean removeInput(@Nonnull InputConnector input) {
        Objects.requireNonNull(input);
        if (!inputs.containsKey(input.getId())) {
            return false;
        }

        //When removing an input, the output connected to it should be disconnected
        workflow.disconnect(input);

        var removeSuccess = inputs.remove(input.getId()) != null;
        if (removeSuccess) {
            workflow.nodeModified(this);
        }
        return removeSuccess;
    }

    protected boolean removeOutput(@Nonnull OutputConnector output) {
        Objects.requireNonNull(output);
        if (!outputs.containsKey(output.getId())) {
            return false;
        }

        //When removing an output, all the inputs connected to it should be disconnected
        output.getConnectedTo().forEach(workflow::disconnect);

        var removeSuccess = outputs.remove(output.getId()) != null;
        if (removeSuccess) {
            workflow.nodeModified(this);
        }
        return removeSuccess;
    }

    public <T extends InputConnector> T addInputConnector(@Nonnull Function<Integer, T> connectorSupplier) {
        Objects.requireNonNull(connectorSupplier);
        var connector = connectorSupplier.apply(currentId.incrementAndGet());
        inputs.put(connector.getId(), connector);
        workflow.nodeModified(this);
        return connector;
    }

    public <T extends OutputConnector> T addOutputConnector(@Nonnull Function<Integer, T> connectorSupplier) {
        Objects.requireNonNull(connectorSupplier);
        var connector = connectorSupplier.apply(currentId.incrementAndGet());
        outputs.put(connector.getId(), connector);
        workflow.nodeModified(this);
        return connector;
    }

    public abstract NodeArguments execute(@Nonnull NodeArguments arguments);

    protected Connector.Builder getConnectorBuilder() {
        return connectorBuilder;
    }

    public void connectorModified(@Nonnull Connector connector) {
        workflow.nodeModified(connector.getParent());
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    @Override
    public String toString() {
        return "Node (" + getId() + ")";
    }
}
