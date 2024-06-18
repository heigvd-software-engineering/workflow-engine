package com.heig.entities;

import jakarta.annotation.Nonnull;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.builder.GraphBuilder;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Workflow {
    private final AtomicInteger currentId = new AtomicInteger(0);
    private final ConcurrentMap<Integer, Node> nodes = new ConcurrentHashMap<>();

    public Node createNode() {
        var node = new Node(currentId.incrementAndGet(), this);
        nodes.put(node.getId(), node);
        return node;
    }

    public boolean removeNode(Node node) {
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
                throw new RuntimeException("Could not remove input" + input + " from " + currentOutput);
            }
        });
        input.setConnectedTo(null);
        return true;
    }

    /**
     * For the workflow to be valid we need all nodes to be connected one way or another to each other.
     * A workflow with no nodes is considered not valid (it cannot be executed).
     * The graph cannot contain cycles.
     * @return Whether the workflow is valid or not
     */
    public boolean isValid() {
        if (nodes.isEmpty()) {
            return false;
        }

        //Creating a directed graph with the vertices and edges currently in our graph
        var directedGraph = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
        nodes.values().forEach(node -> directedGraph.addVertex(node.getId()));
        nodes.values()
                .forEach(node ->
                        node.getOutputs().values()
                                .forEach(output ->
                                        output.getConnectedTo()
                                                .forEach(input ->
                                                        directedGraph.addEdge(node.getId(), input.getParent().getId())
                                                )
                                )
                );

        //Cycle detection
        var cycleDetector = new CycleDetector<>(directedGraph);
        if (cycleDetector.detectCycles()) {
            return false;
        }

        //Checks that the graph is weakly connected
        var ci = new ConnectivityInspector<>(directedGraph);
        if (!ci.isConnected()) {
            return false;
        }

        //Check the types compatibility
        for (var node : nodes.values()) {
            for (var output : node.getOutputs().values()) {
                for (var input : output.getConnectedTo()) {
                    if (!output.getType().isCompatibleWith(input.getType())) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
}
