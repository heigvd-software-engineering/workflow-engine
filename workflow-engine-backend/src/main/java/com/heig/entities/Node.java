package com.heig.entities;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Node {
    private int id = -1;
    private boolean isDeterministic = false;
    private int timeout = 5000;

    private final List<InputConnector> inputs = new LinkedList<>();
    private final List<OutputConnector> outputs = new LinkedList<>();

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

    public void setId(int id) {
        this.id = id;
    }

    public List<InputConnector> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    public List<OutputConnector> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    public boolean addInput(@Nonnull InputConnector input) {
        Objects.requireNonNull(input);
        if (inputs.contains(input)) {
            return false;
        }
        return inputs.add(input);
    }

    public boolean removeInput(@Nonnull InputConnector input) {
        Objects.requireNonNull(input);
        if (!inputs.contains(input)) {
            return false;
        }

        //When removing an input, the output connected to it should be disconnected
        Node.disconnect(input);

        return inputs.remove(input);
    }

    public boolean addOutput(@Nonnull OutputConnector output) {
        Objects.requireNonNull(output);
        if (outputs.contains(output)) {
            return false;
        }
        return outputs.add(output);
    }

    public boolean removeOutput(@Nonnull OutputConnector output) {
        Objects.requireNonNull(output);
        if (!outputs.contains(output)) {
            return false;
        }

        //When removing an output, all the inputs connected to it should be disconnected
        output.getConnectedTo().forEach(Node::disconnect);

        return outputs.remove(output);
    }

    public static boolean connect(@Nonnull OutputConnector output, @Nonnull InputConnector input) {
        Objects.requireNonNull(output);
        Objects.requireNonNull(input);

        //Connects the output to the input
        if (!output.addConnectedTo(input)) {
            return false;
        }

        //If the input is already connected to an output, we disconnect it
        if (!Node.disconnect(input)) {
            return false;
        }

        //We connect the input to the output
        input.setConnectedTo(output);
        return true;
    }

    public static boolean disconnect(@Nonnull InputConnector input) {
        Objects.requireNonNull(input);

        input.getConnectedTo().ifPresent(currentOutput -> {
            if (!currentOutput.removeConnectedTo(input)) {
                //Should never happen
                throw new RuntimeException("Could not remove input" + input + " from " + currentOutput);
            }
        });
        input.setConnectedTo(null);
        return true;
    }
}
