package com.heig.entities.workflow.nodes;

import com.heig.entities.workflow.connectors.Connector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
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
        public Builder(Workflow workflow) {
            this.workflow = workflow;
        }

        public CodeNode buildCodeNode() {
            return workflow.addNode((id) -> new CodeNode(id, workflow));
        }

        public PrimitiveNode buildPrimitiveNode(@Nonnull WType type) {
            return workflow.addNode((id) -> new PrimitiveNode(id, workflow, type));
        }
    }

    private final Connector.Builder connectorBuilder = new Connector.Builder(this);
    private final AtomicInteger currentId = new AtomicInteger(0);

    private boolean isDeterministic = false;
    private int timeout = 5000;

    private final int id;
    private final Workflow workflow;
    private final ConcurrentMap<Integer, InputConnector> inputs = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, OutputConnector> outputs = new ConcurrentHashMap<>();

    protected Node(int id, @Nonnull Workflow workflow) {
        if (id < 0) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(workflow);
        this.id = id;
        this.workflow = workflow;
    }

    public boolean getDeterministic() {
        return isDeterministic;
    }

    protected void setDeterministic(boolean deterministic) {
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
    }

    protected boolean removeInput(@Nonnull InputConnector input) {
        Objects.requireNonNull(input);
        if (!inputs.containsKey(input.getId())) {
            return false;
        }

        //When removing an input, the output connected to it should be disconnected
        workflow.disconnect(input);

        return inputs.remove(input.getId()) != null;
    }

    protected boolean removeOutput(@Nonnull OutputConnector output) {
        Objects.requireNonNull(output);
        if (!outputs.containsKey(output.getId())) {
            return false;
        }

        //When removing an output, all the inputs connected to it should be disconnected
        output.getConnectedTo().forEach(workflow::disconnect);

        return outputs.remove(output.getId()) != null;
    }

    public InputConnector addInputConnector(Function<Integer, InputConnector> connectorSupplier) {
        var connector = connectorSupplier.apply(currentId.incrementAndGet());
        inputs.put(connector.getId(), connector);
        return connector;
    }

    public OutputConnector addOutputConnector(Function<Integer, OutputConnector> connectorSupplier) {
        var connector = connectorSupplier.apply(currentId.incrementAndGet());
        outputs.put(connector.getId(), connector);
        return connector;
    }

    @Nonnull
    public abstract NodeArguments execute(@Nonnull NodeArguments arguments);

    protected Connector.Builder getConnectorBuilder() {
        return connectorBuilder;
    }
}
