package com.heig.entities.workflow.execution;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.errors.WorkflowErrors;
import com.heig.entities.workflow.nodes.Node;
import com.heig.helpers.CustomJsonDeserializer;
import com.heig.helpers.CustomJsonSerializer;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Represents the state that a {@link Node} has
 */
public class NodeState {
    /**
     * Used to convert a {@link NodeState} to a json representation
     */
    public static class Serializer implements CustomJsonSerializer<NodeState> {
        @Override
        public JsonElement serialize(NodeState value) {
            var obj = new JsonObject();
            obj.addProperty("nodeId", value.getNode().getId());
            obj.addProperty("posX", value.getPosition().x);
            obj.addProperty("posY", value.getPosition().y);
            obj.addProperty("hasBeenModified", value.hasBeenModified());
            return obj;
        }
    }

    /**
     * Used to convert a json to a {@link NodeState}
     */
    public static class Deserializer implements CustomJsonDeserializer<NodeState> {
        private final Workflow workflow;
        public Deserializer(Workflow workflow) {
            this.workflow = workflow;
        }

        @Override
        public NodeState deserialize(JsonElement value) throws JsonParseException {
            var obj = value.getAsJsonObject();
            var nodeId = obj.get("nodeId").getAsInt();
            var posX = obj.get("posX").getAsDouble();
            var posY = obj.get("posY").getAsDouble();
            var hasBeenModified = obj.get("hasBeenModified").getAsBoolean();

            var nodeOpt = workflow.getNode(nodeId);
            if (nodeOpt.isEmpty()) {
                throw new JsonParseException("Node with id " + nodeId + " does not exist");
            }
            var node = nodeOpt.get();
            var nodeState = new NodeState(node);
            nodeState.setPosition(new Point2D.Double(posX, posY));
            nodeState.setHasBeenModified(hasBeenModified);

            return nodeState;
        }
    }

    /**
     * The current state of the {@link NodeState}
     */
    private State state = State.IDLE;

    /**
     * The input values that have been passed to this node
     */
    private final Map<Integer, Object> valuesMap = new HashMap<>();

    /**
     * The execution errors for this node
     */
    private WorkflowErrors errors = null;

    /**
     * The {@link Node} linked to this {@link NodeState}
     */
    private final Node node;

    /**
     * Whether the node has been modified
     */
    private boolean hasBeenModified = false;

    /**
     * The position of the node on the UI
     */
    private final Point.Double position = new Point.Double(0, 0);

    public NodeState(@Nonnull Node node) {
        this.node = Objects.requireNonNull(node);
    }

    public synchronized void setInputValue(int connectorId, Object value) {
        valuesMap.put(connectorId, value);
    }

    /**
     * Returns the input value for a specific connector
     * @param connectorId The id of the connector
     * @return Returns :
     * <ul>
     *     <li>{@link Optional#empty()} if the value for the connector does not exist</li>
     *     <li>{@link Optional#of(Object)} of {@link Optional#empty()} if the value exists but is null</li>
     *     <li>{@link Optional#of(Object)} of {@link Optional#of(Object)} if the value exists and is not null</li>
     * </ul>
     */
    public synchronized Optional<Optional<Object>> getInputValue(int connectorId) {
        if (!valuesMap.containsKey(connectorId)) {
            return Optional.empty();
        }

        var value = valuesMap.get(connectorId);
        return Optional.of(value == null ? Optional.empty() : Optional.of(value));
    }

    public Map<Integer, Object> getValues() {
        return Collections.unmodifiableMap(valuesMap);
    }

    public Node getNode() {
        return node;
    }

    /**
     * Returns true if the node is ready to be executed (all the necessary inputs values are presents)
     * @return True if the node is ready to be executed, false otherwise
     */
    public boolean isReady() {
        //For the node to be ready to be executed, we need to have all inputs (except the ones marked as optional) to be available
        //If the input is marked as optional but is connected to an output, we need it to check the readiness of the node
        return node.getInputs().values().stream()
            .filter(i -> !(i.isOptional() && i.getConnectedTo().isEmpty()))
            .allMatch(c -> valuesMap.containsKey(c.getId()));
    }

    public synchronized void setErrors(@Nonnull WorkflowErrors error) {
        this.errors = Objects.requireNonNull(error);
    }

    public synchronized Optional<WorkflowErrors> getErrors() {
        return Optional.ofNullable(errors);
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

    public synchronized void clearInputs() {
        valuesMap.clear();
    }

    public void setPosition(@Nonnull Point.Double pos) {
        Objects.requireNonNull(pos);

        this.position.setLocation(pos);
    }

    public Point.Double getPosition() {
        return position;
    }
}
