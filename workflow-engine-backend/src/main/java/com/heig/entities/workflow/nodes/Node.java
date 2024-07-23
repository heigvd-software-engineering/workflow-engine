package com.heig.entities.workflow.nodes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.heig.entities.workflow.connectors.Connector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.types.WPrimitive;
import com.heig.helpers.CustomJsonDeserializer;
import com.heig.helpers.CustomJsonSerializer;
import com.heig.helpers.Utils;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a node
 */
public abstract class Node {
    /**
     * Used to deserialize a specific node from a json representation
     * @param <T> The class implementing {@link Node}
     */
    public static abstract class NodeDeserializer<T extends Node> implements CustomJsonDeserializer<T> {
        protected int id;
        protected Workflow workflow;
        public NodeDeserializer(int id, Workflow workflow) {
            this.id = id;
            this.workflow = workflow;
        }
    }

    /**
     * Used to serialize a {@link Node}. Uses {@link Node#toJson()}.
     */
    public static class Serializer implements CustomJsonSerializer<Node> {
        @Override
        public JsonElement serialize(Node value) {
            var obj = value.toJson();
            obj.addProperty("currentId", value.currentId.get());
            return obj;
        }
    }

    /**
     * Used to deserialize a {@link Node}
     */
    public static class Deserializer implements CustomJsonDeserializer<Node> {
        private final Workflow workflow;
        private final Utils.Connexions connexionsToMake;
        public Deserializer(Utils.Connexions connexionsToMake, Workflow workflow) {
            this.connexionsToMake = connexionsToMake;
            this.workflow = workflow;
        }

        @Override
        public Node deserialize(JsonElement value) throws JsonParseException {
            var obj = value.getAsJsonObject();
            var currentId = obj.get("currentId").getAsInt();
            var id = obj.get("id").getAsInt();
            var isDeterministic = obj.get("isDeterministic").getAsBoolean();
            var timeout = obj.get("timeout").getAsInt();

            var nodeType = obj.get("nodeType").getAsString();
            var deserializer = switch (nodeType) {
                case "CodeNode":
                    yield new CodeNode.Deserializer(id, workflow);
                case "PrimitiveNode":
                    yield new PrimitiveNode.Deserializer(id, workflow);
                case "FileNode":
                    yield new FileNode.Deserializer(id, workflow);
                default:
                    throw new RuntimeException("The node type '%s' was not found".formatted(nodeType));
            };

            var node = deserializer.deserialize(obj);

            //Only used when loading workflow from disk. This is to avoid having connectors already created in the node constructor and then trying to add the same
            node.removeAllConnectors();

            var inputs = Utils.deserializeList(new Connector.Deserializer(connexionsToMake, node, true), obj.get("inputs").getAsJsonArray());
            var outputs = Utils.deserializeList(new Connector.Deserializer(connexionsToMake, node, false), obj.get("outputs").getAsJsonArray());

            node.setConnectors(
                inputs.stream().map(c -> (InputConnector) c).toList(),
                outputs.stream().map(c -> (OutputConnector) c).toList()
            );
            node.setInfos(currentId, isDeterministic, timeout);

            return node;
        }
    }

    /**
     * Builder allows to create nodes for a workflow
     */
    public static class Builder {
        private final Workflow workflow;
        public Builder(@Nonnull Workflow workflow) {
            this.workflow = Objects.requireNonNull(workflow);
        }

        /**
         * Builds a {@link CodeNode}
         * @return A {@link CodeNode}
         */
        public CodeNode buildCodeNode() {
            return workflow.addNode((id) -> new CodeNode(id, workflow));
        }

        /***
         * Builds a {@link PrimitiveNode} with a specific {@link WPrimitive} type
         * @param type The primitive type
         * @return A {@link PrimitiveNode}
         */
        public PrimitiveNode buildPrimitiveNode(@Nonnull WPrimitive type) {
            return workflow.addNode((id) -> new PrimitiveNode(id, workflow, type));
        }

        /**
         * Builds a {@link FileNode}
         * @return A {@link FileNode}
         */
        public FileNode buildFileNode() {
            return workflow.addNode((id) -> new FileNode(id, workflow));
        }
    }

    /**
     * The Builder to build connectors
     */
    protected final Connector.Builder connectorBuilder;

    /**
     * The current id used to generate connectors
     */
    private final AtomicInteger currentId = new AtomicInteger(0);

    /**
     * Whether the node is deterministic or not
     */
    private boolean isDeterministic = false;

    /**
     * The execution timeout in ms
     */
    private int timeout = 5000;

    /**
     * The id of the node
     */
    private final int id;

    /**
     * The workflow the node is contained in
     */
    private final Workflow workflow;

    /**
     * The inputs connectors
     */
    private final ConcurrentMap<Integer, InputConnector> inputs = new ConcurrentHashMap<>();

    /**
     * The outputs connectors
     */
    private final ConcurrentMap<Integer, OutputConnector> outputs = new ConcurrentHashMap<>();

    //region Deserialization

    /**
     * Used only in the Deserialization process.
     * Sets multiple information about the current node
     * @param currentId The current id for the connectors
     * @param isDeterministic Whether the node is deterministic or not
     * @param timeout The timeout in ms
     */
    private void setInfos(int currentId, boolean isDeterministic, int timeout) {
        this.currentId.set(currentId);
        this.isDeterministic = isDeterministic;
        this.timeout = timeout;
    }

    /**
     * Used only in the Deserialization process.
     * Sets the inputs and outputs
     * @param inputs The inputs
     * @param outputs The outputs
     */
    private void setConnectors(List<InputConnector> inputs, List<OutputConnector> outputs) {
        for (var inputConnector : inputs) {
            this.inputs.put(inputConnector.getId(), inputConnector);
        }

        for (var outputConnector : outputs) {
            this.outputs.put(outputConnector.getId(), outputConnector);
        }
    }

    /**
     * Used only in the Deserialization process.
     * Removes all the connectors
     */
    private void removeAllConnectors() {
        this.inputs.clear();
        this.outputs.clear();
    }

    //endregion

    protected Node(int id, @Nonnull Workflow workflow, boolean areConnectorsReadOnly) {
        if (id < 0) {
            throw new IllegalArgumentException("The id cannot be negative");
        }
        this.id = id;
        this.workflow = Objects.requireNonNull(workflow);
        this.connectorBuilder = new Connector.Builder(this, areConnectorsReadOnly);
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

    /**
     * Sets the timeout. Throws an {@link IllegalArgumentException} if the timeout value is smaller or equals than 0
     * @param timeout The timeout in ms
     */
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

    /**
     * Disconnects every node connected (all inputs and all outputs).
     * Notifies {@link Workflow#nodeModified(Node)}.
     */
    public void disconnectEverything() {
        inputs.values().forEach(workflow::disconnect);
        outputs.values().forEach(output -> output.getConnectedTo().forEach(workflow::disconnect));
        workflow.nodeModified(this);
    }

    /**
     * Removes an input.
     * Notifies {@link Workflow#nodeModified(Node)}.
     * @param input The input to disconnect
     * @return False if the input could not be removed, true otherwise
     */
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

    /**
     * Removes an output.
     * Notifies {@link Workflow#nodeModified(Node)}.
     * @param output The output to disconnect
     * @return False if the output could not be removed, true otherwise
     */
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

    /**
     * Adds a new {@link InputConnector}.
     * Notifies {@link Workflow#nodeModified(Node)}.
     * @param connectorSupplier A function taking an id in parameter and return an object of type T
     * @return The connector added
     * @param <T> The type of the {@link InputConnector}
     */
    public <T extends InputConnector> T addInputConnector(@Nonnull Function<Integer, T> connectorSupplier) {
        Objects.requireNonNull(connectorSupplier);
        var connector = connectorSupplier.apply(currentId.incrementAndGet());
        inputs.put(connector.getId(), connector);
        workflow.nodeModified(this);
        return connector;
    }

    /**
     * Adds a new {@link OutputConnector}.
     * Notifies {@link Workflow#nodeModified(Node)}.
     * @param connectorSupplier A function taking an id in parameter and return an object of type T
     * @return The connector added
     * @param <T> The type of the {@link OutputConnector}
     */
    public <T extends OutputConnector> T addOutputConnector(@Nonnull Function<Integer, T> connectorSupplier) {
        Objects.requireNonNull(connectorSupplier);
        var connector = connectorSupplier.apply(currentId.incrementAndGet());
        outputs.put(connector.getId(), connector);
        workflow.nodeModified(this);
        return connector;
    }

    /**
     * Executes the node
     * @param inputs The inputs
     * @param logLine The function allowing to log
     * @return The outputs
     */
    public abstract NodeArguments execute(@Nonnull NodeArguments inputs, @Nonnull Consumer<String> logLine);

    protected Connector.Builder getConnectorBuilder() {
        return connectorBuilder;
    }

    /**
     * Notifies that a connector has been modified (in this version it calls directly {@link Workflow#nodeModified(Node)}).
     * Notifies {@link Workflow#nodeModified(Node)}.
     * @param connector
     */
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

    /**
     * The representation of the node in the json format
     * @return The representation of the node in the json format
     */
    public JsonObject toJson() {
        var obj = new JsonObject();
        obj.addProperty("nodeType", getClass().getSimpleName());
        obj.addProperty("id", id);
        obj.addProperty("isDeterministic", isDeterministic);
        obj.addProperty("timeout", timeout);

        var inputsArr = new JsonArray();
        for (var inputConnector : inputs.values().stream().sorted(Comparator.comparingInt(InputConnector::getId)).toList()) {
            inputsArr.add(inputConnector.toJson());
        }
        obj.add("inputs", inputsArr);

        var outputsArr = new JsonArray();
        for (var outputConnector : outputs.values().stream().sorted(Comparator.comparingInt(OutputConnector::getId)).toList()) {
            outputsArr.add(outputConnector.toJson());
        }
        obj.add("outputs", outputsArr);
        return obj;
    }

    /**
     * Function executed when the execution is finished or cancelled
     */
    public void cancel() { }
}
