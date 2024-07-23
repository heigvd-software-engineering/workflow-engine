package com.heig.entities.workflow;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.errors.*;
import com.heig.entities.workflow.nodes.Node;
import com.heig.helpers.CustomJsonDeserializer;
import com.heig.helpers.CustomJsonSerializer;
import com.heig.helpers.Utils;
import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.annotation.Nonnull;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class Workflow {
    public static class Serializer implements CustomJsonSerializer<Workflow> {
        @Override
        public JsonElement serialize(Workflow value) {
            var obj = new JsonObject();
            obj.addProperty("currentId", value.currentId.get());
            obj.add("nodes", Utils.serializeList(new Node.Serializer(), value.nodes.values().stream().toList()));
            obj.addProperty("uuid", value.getUUID().toString());
            obj.addProperty("name", value.getName());
            return obj;
        }
    }

    public static class Deserializer implements CustomJsonDeserializer<Workflow> {
        @Override
        public Workflow deserialize(JsonElement value) throws JsonParseException {
            var obj = value.getAsJsonObject();
            var currentId = obj.get("currentId").getAsInt();
            var uuid = obj.get("uuid").getAsString();
            var name = obj.get("name").getAsString();
            var workflow = new Workflow(currentId, uuid, name);

            Utils.Connexions connexionsToMake = new Utils.Connexions(new LinkedList<>());
            var nodes = Utils.deserializeList(new Node.Deserializer(connexionsToMake, workflow), obj.get("nodes").getAsJsonArray());
            workflow.setNodes(nodes);

            for (var connexion : connexionsToMake.connexions()) {
                var outputConnectorOpt = workflow
                    .getNode(connexion.output().nodeId())
                    .flatMap(n -> n.getOutput(connexion.output().connectorId()));
                if (outputConnectorOpt.isEmpty()) {
                    continue;
                }

                var inputConnectorOpt = workflow
                    .getNode(connexion.input().nodeId())
                    .flatMap(n -> n.getInput(connexion.input().connectorId()));
                if (inputConnectorOpt.isEmpty()) {
                    continue;
                }

                workflow.connect(outputConnectorOpt.get(), inputConnectorOpt.get());
            }

            return workflow;
        }
    }

    private final AtomicInteger currentId = new AtomicInteger(0);
    private final ConcurrentMap<Integer, Node> nodes = new ConcurrentHashMap<>();
    private final Node.Builder nodeBuilder = new Node.Builder(this);
    private final UUID uuid;
    private final String name;
    private final ConcurrentHashSet<NodeModifiedListener> listeners = new ConcurrentHashSet<>();

    //region Deserialization

    private Workflow(int currentId, String uuid, String name) {
        this.uuid = UUID.fromString(uuid);
        this.name = name;
        this.currentId.set(currentId);
    }

    private void setNodes(List<Node> nodes) {
        for (var node: nodes) {
            this.nodes.put(node.getId(), node);
        }
    }

    //endregion

    public Workflow(@Nonnull String name) {
        this.uuid = UUID.randomUUID();
        this.name = Objects.requireNonNull(name);
    }

    public Node.Builder getNodeBuilder() {
        return nodeBuilder;
    }

    public <T extends Node> T addNode(@Nonnull Function<Integer, T> nodeCreator) {
        Objects.requireNonNull(nodeCreator);
        var node = nodeCreator.apply(currentId.incrementAndGet());
        nodes.put(node.getId(), node);
        return node;
    }

    public boolean removeNode(@Nonnull Node node) {
        Objects.requireNonNull(node);
        //When removing a node we need to disconnect everything connected to it
        node.disconnectEverything();

        return nodes.remove(node.getId()) != null;
    }

    public Map<Integer, Node> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public Optional<Node> getNode(int id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public boolean connect(@Nonnull OutputConnector output, @Nonnull InputConnector input) {
        Objects.requireNonNull(output);
        Objects.requireNonNull(input);

        //You cannot connect an input and an output of the same node
        if (output.getParent() == input.getParent()) {
            return false;
        }

        //Connects the output to the input
        if (!output.addConnectedTo(input)) {
            return false;
        }

        //If the input is already connected to an output, we disconnect it
        if (!disconnect(input)) {
            return false;
        }

        //We connect the input to the output
        input.setConnectedTo(output);
        return true;
    }

    public boolean disconnect(@Nonnull InputConnector input) {
        Objects.requireNonNull(input);

        input.getConnectedTo().ifPresent(currentOutput -> {
            if (!currentOutput.removeConnectedTo(input)) {
                //Should never happen
                throw new RuntimeException("Could not disconnect input" + input + " from " + currentOutput);
            }
        });
        input.setConnectedTo(null);
        return true;
    }

    /**
     * For the workflow to be valid we need all nodes to be connected one way or another to each other.
     * A workflow with no nodes is considered not valid (it cannot be executed).
     * The graph cannot contain cycles.
     * @return The errors if there are some
     */
    public Optional<WorkflowErrors> isValid() {
        var errors = new WorkflowErrors();
        if (nodes.isEmpty()) {
            errors.addError(new EmptyGraph());
        }

        //Creating a directed graph with the vertices and edges currently in our graph
        var directedGraph = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
        for (var node : nodes.values()) {
            directedGraph.addVertex(node.getId());
        }

        for (var node : nodes.values()) {
            for (var output : node.getOutputs().values()) {
                for (var input : output.getConnectedTo()) {
                    var source = node.getId();
                    var target = input.getParent().getId();
                    if (!directedGraph.containsEdge(source, target)) {
                        directedGraph.addEdge(source, target);
                    }
                }
            }
        }

        //Cycle detection
        var cycleDetector = new CycleDetector<>(directedGraph);
        if (cycleDetector.detectCycles()) {
            errors.addError(new CycleDetected());
        }

        //Checks that the graph is weakly connected
        var ci = new ConnectivityInspector<>(directedGraph);
        if (!ci.isConnected()) {
            errors.addError(new NotConnectedGraph());
        }

        //All inputs not marked as optional should be connected to an output
        for (var node : nodes.values()) {
            for (var input : node.getInputs().values().stream().filter(i -> !i.isOptional()).toList()) {
                if (input.getConnectedTo().isEmpty()) {
                    errors.addError(new InputNotConnected(input));
                }
            }
        }

        //Check the types compatibility
        for (var node : nodes.values()) {
            for (var output : node.getOutputs().values()) {
                for (var input : output.getConnectedTo()) {
                    if (!input.getType().canBeConvertedFrom(output.getType())) {
                        errors.addError(new IncompatibleTypes(input, output));
                    }
                }
            }
        }

        if (!errors.getErrors().isEmpty()) {
            return Optional.of(errors);
        }
        return Optional.empty();
    }

    public void addNodeModifiedListener(@Nonnull NodeModifiedListener consumer) {
        Objects.requireNonNull(consumer);
        listeners.add(consumer);
    }

    public void removeNodeModifiedListener(@Nonnull NodeModifiedListener consumer) {
        Objects.requireNonNull(consumer);
        listeners.remove(consumer);
    }

    public void nodeModified(@Nonnull Node node) {
        Objects.requireNonNull(node);
        for (var listener : listeners) {
            listener.nodeModified(node);
        }
    }
}
