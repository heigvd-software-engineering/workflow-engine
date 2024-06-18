package com.heig.entities;

import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Node {
    private final AtomicInteger currentId = new AtomicInteger(0);

    private final int id;
    private boolean isDeterministic = false;
    private int timeout = 5000;
    private final Workflow workflow;

    private final ConcurrentMap<Integer, InputConnector> inputs = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, OutputConnector> outputs = new ConcurrentHashMap<>();

    Node(int id, Workflow workflow) {
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

    public void setDeterministic(boolean deterministic) {
        isDeterministic = deterministic;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
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

    public boolean removeInput(@Nonnull InputConnector input) {
        Objects.requireNonNull(input);
        if (!inputs.containsKey(input.getId())) {
            return false;
        }

        //When removing an input, the output connected to it should be disconnected
        workflow.disconnect(input);

        return inputs.remove(input.getId()) != null;
    }

    public boolean removeOutput(@Nonnull OutputConnector output) {
        Objects.requireNonNull(output);
        if (!outputs.containsKey(output.getId())) {
            return false;
        }

        //When removing an output, all the inputs connected to it should be disconnected
        output.getConnectedTo().forEach(workflow::disconnect);

        return outputs.remove(output.getId()) != null;
    }

    public InputConnector createInputConnector() {
        var connector = new InputConnector(currentId.incrementAndGet(), this);
        inputs.put(connector.getId(), connector);
        return connector;
    }

    public OutputConnector createOutputConnector() {
        var connector = new OutputConnector(currentId.incrementAndGet(), this);
        outputs.put(connector.getId(), connector);
        return connector;
    }
}
