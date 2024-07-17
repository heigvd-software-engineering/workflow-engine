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
import com.heig.entities.workflow.types.WType;
import com.heig.helpers.CustomJsonDeserializer;
import com.heig.helpers.CustomJsonSerializer;
import com.heig.helpers.Utils;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Node {
    public static abstract class NodeDeserializer<T> implements CustomJsonDeserializer<T> {
        protected int id;
        protected Workflow workflow;
        public NodeDeserializer(int id, Workflow workflow) {
            this.id = id;
            this.workflow = workflow;
        }
    }

    public static class Serializer implements CustomJsonSerializer<Node> {
        @Override
        public JsonElement serialize(Node value) {
            var obj = value.toJson();
            obj.addProperty("currentId", value.currentId.get());
            return obj;
        }
    }

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

        public FileNode buildFileNode() {
            return workflow.addNode((id) -> new FileNode(id, workflow));
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

    //region Deserialization

    private void setInfos(int currentId, boolean isDeterministic, int timeout) {
        this.currentId.set(currentId);
        this.isDeterministic = isDeterministic;
        this.timeout = timeout;
    }

    private void setConnectors(List<InputConnector> inputs, List<OutputConnector> outputs) {
        for (var inputConnector : inputs) {
            this.inputs.put(inputConnector.getId(), inputConnector);
        }

        for (var outputConnector : outputs) {
            this.outputs.put(outputConnector.getId(), outputConnector);
        }
    }

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

    public abstract NodeArguments execute(@Nonnull NodeArguments arguments, @Nonnull Consumer<String> logLine);

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
    public void clean() { }
}
