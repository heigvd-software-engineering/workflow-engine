package com.heig.resources;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.heig.entities.workflow.NodeModifiedListener;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.execution.*;
import com.heig.entities.workflow.nodes.CodeNode;
import com.heig.entities.workflow.nodes.ModifiableNode;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.nodes.PrimitiveNode;
import com.heig.helpers.ResultOrStringError;
import com.heig.services.WorkflowService;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 *
 * Source: <a href="https://quarkus.io/guides/websockets">Using websockets</a>
 */
@ServerEndpoint("/workflow")
@ApplicationScoped
public class WorkflowSocket {
    @Inject
    WorkflowService service;

    private final ConcurrentMap<Session, Optional<UUID>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Listener> listeners = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        WorkflowManager.loadExistingWorkflows(uuid -> {
            var listener = new Listener(uuid);
            return Tuple2.of(listener, we -> {
                we.getWorkflow().addNodeModifiedListener(listener);
                listeners.put(we.getWorkflow().getUUID(), listener);
            });
        });
    }

    /**
     * Class used to transmit changes to the concerned {@link Session}
     */
    private class Listener implements WorkflowExecutionListener, NodeModifiedListener {
        /**
         * The log content
         */
        private String log = "";

        /**
         * The workflow UUID
         */
        private final UUID uuid;

        public Listener(@Nonnull UUID uuid) {
            this.uuid = Objects.requireNonNull(uuid);
        }

        /**
         * Returns a list of {@link Session} concerned
         * @return The list of {@link Session} concerned
         */
        private Stream<Session> toNotify() {
            var optUUID = Optional.of(uuid);
            return sessions.entrySet().stream()
                .filter(es -> es.getValue().equals(optUUID))
                .map(Map.Entry::getKey);
        }

        /**
         * Notifies the {@link Session} concerned
         * @param message The notification to send
         */
        private void notifyConcerned(String message) {
            toNotify().forEach(s -> sendTo(s, message));
        }

        @Override
        public void nodeModified(@Nonnull Node node) {
            //We only notify of a node modification if the node is present in the workflow
            node.getWorkflow().getNode(node.getId()).ifPresent(n ->
                notifyConcerned(nodeModifiedJson(n))
            );
        }

        @Override
        public void workflowStateChanged(@Nonnull WorkflowExecutor we) {
            notifyConcerned(workflowStateJson(we));
        }

        @Override
        public void nodeStateChanged(@Nonnull NodeState state) {
            notifyConcerned(nodeStateJson(state));
        }

        /**
         * Notifies the {@link Session} concerned when a node was created
         * @param node The node created
         */
        public void notifyNodeCreated(@Nonnull Node node) {
            notifyConcerned(nodeModifiedJson(node));
        }

        /**
         * Notifies the {@link Session} concerned when a node was removed
         * @param node The node removed
         */
        public void notifyNodeRemoved(@Nonnull Node node) {
            notifyConcerned(nodeRemovedJson(node));
        }

        @Override
        public synchronized void newLogLine(@Nonnull String line) {
            log += line;
            notifyConcerned(logJson(log));
        }

        @Override
        public synchronized void clearLog() {
            log = "";
            notifyConcerned(logJson(log));
        }

        /**
         * Returns the current text in the log
         * @return The current text in the log
         */
        public synchronized String getLog() {
            return log;
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        //Send all the currently available workflows
        sessions.put(session, Optional.empty());
        sendTo(session, allWorkflowsJson());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);
        System.out.println("Session error: " + throwable);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        var g = new Gson();
        try {
            var obj = g.fromJson(message, JsonElement.class).getAsJsonObject();
            var name = obj.get("action").getAsString();
            ResultOrStringError<Void> res = switch (name) {
                case "createWorkflow" -> {
                    var w = service.createWorkflow(obj.get("name"));
                    var listener = new Listener(w.getUUID());
                    var we = service.createWorkflowExecutor(w, listener);
                    we.getWorkflow().addNodeModifiedListener(listener);
                    listeners.put(w.getUUID(), listener);
                    //Broadcast that a new workflow is available
                    broadcast(newWorkflowJson(w));
                    yield ResultOrStringError.result(null);
                }
                case "executeWorkflow" ->
                    service.getWorkflowExecutor(obj.get("uuid")).continueWith(we ->
                        service.executeWorkflow(we)
                    );
                case "saveWorkflow" ->
                    service.getWorkflowExecutor(obj.get("uuid")).continueWith(we ->
                        service.saveWorkflowExecutor(we)
                    );
                case "removeWorkflow" ->
                    service.getWorkflowExecutor(obj.get("uuid")).continueWith(we -> {
                        var listenerToRemove = listeners.remove(we.getWorkflow().getUUID());
                        we.getWorkflow().removeNodeModifiedListener(listenerToRemove);
                        return service.removeWorkflowExecutor(we).continueWith(v -> {
                            //Broadcast that the workflow is no longer available
                            broadcast(deletedWorkflowJson(we.getWorkflow()));
                            return ResultOrStringError.result(null);
                        });
                    });
                case "stopWorkflow" ->
                    service.getWorkflowExecutor(obj.get("uuid")).continueWith(we ->
                       service.stopWorkflow(we)
                    );
                case "switchTo" ->
                    service.getOptionalWorkflowExecutor(obj.get("uuid")).continueWith(weOpt -> {
                        sessions.put(session, Optional.empty());
                        //Here send the current state of the new workflow (all nodes, connectors, state, ...)
                        sendTo(session, switchedToJson(weOpt.map(wee -> wee.getWorkflow().getUUID().toString()).orElse("")));

                        weOpt.ifPresent(we -> {
                            sendTo(session, logJson(listeners.get(we.getWorkflow().getUUID()).getLog()));
                            sendTo(session, workflowStateJson(we));
                            we.getWorkflow().getNodes().values().forEach(n -> {
                                sendTo(session, nodeModifiedJson(n));
                                sendTo(session, nodeStateJson(we.getStateFor(n)));
                            });
                            sessions.put(session, Optional.of(we.getWorkflow().getUUID()));
                        });
                        return ResultOrStringError.result(null);
                    });
                case "createNode" ->
                    service.getWorkflowExecutor(obj.get("uuid")).continueWith(we ->
                        service.createNode(we, obj.get("type"), obj.has("primitive") ? obj.get("primitive") : null, obj.get("posX"), obj.get("posY")).continueWith(n -> {
                            //Notify node created
                            var listener = listeners.get(we.getWorkflow().getUUID());
                            listener.notifyNodeCreated(n);
                            listener.nodeStateChanged(we.getStateFor(n));
                            return ResultOrStringError.result(null);
                        })
                    );
                case "removeNode" ->
                    service.getWorkflowExecutor(obj.get("uuid")).continueWith(we ->
                        service.getNode(we.getWorkflow(), obj.get("nodeId"), Node.class).continueWith(n ->
                            service.removeNode(we, n).continueWith(v -> {
                                //Notify node removed
                                listeners.get(we.getWorkflow().getUUID()).notifyNodeRemoved(n);
                                return ResultOrStringError.result(null);
                            })
                        )
                    );
                case "moveNode" ->
                    service.getWorkflow(obj.get("uuid")).continueWith(w ->
                        service.getNode(w, obj.get("nodeId"), Node.class).continueWith(n ->
                            service.setNodePosition(n, obj.get("posX"), obj.get("posY"))
                        )
                    );
                case "createConnector" ->
                    service.getWorkflow(obj.get("uuid")).continueWith(w ->
                        service.getNode(w, obj.get("nodeId"), ModifiableNode.class).continueWith(n ->
                            service.createConnector(n, obj.get("isInput"), obj.get("name"), obj.get("type")).continueWith(c ->
                                ResultOrStringError.result(null)
                            )
                        )
                    );
                case "removeConnector" ->
                    service.getWorkflow(obj.get("uuid")).continueWith(w ->
                        service.getNode(w, obj.get("nodeId"), ModifiableNode.class).continueWith(n ->
                            service.getConnector(n, obj.get("connectorId"), obj.get("isInput")).continueWith(c ->
                                service.removeConnector(n, c)
                            )
                        )
                    );
                case "changeConnector" ->
                    service.getWorkflow(obj.get("uuid")).continueWith(w ->
                        service.getNode(w, obj.get("nodeId"), Node.class).continueWith(n ->
                            service.getConnector(n, obj.get("connectorId"), obj.get("isInput")).continueWith(c ->
                                switch (obj.get("subAction").getAsString()) {
                                    case "type" -> service.changeConnectorType(c, obj.get("newType"));
                                    case "name" -> service.changeConnectorName(c, obj.get("newName"));
                                    default -> ResultOrStringError.error("subAction not recognized");
                                }
                            )
                        )
                    );
                case "changeModifiableNode" ->
                    service.getWorkflow(obj.get("uuid")).continueWith(w ->
                        service.getNode(w, obj.get("nodeId"), ModifiableNode.class).continueWith(n ->
                            switch (obj.get("subAction").getAsString()) {
                                case "isDeterministic" -> service.changeModifiableNodeIsDeterministic(n, obj.get("isDeterministic"));
                                case "timeout" -> service.changeModifiableNodeTimeout(n, obj.get("timeout"));
                                default -> ResultOrStringError.error("subAction not recognized");
                            }
                        )
                    );
                case "changePrimitiveNode" ->
                    service.getWorkflow(obj.get("uuid")).continueWith(w ->
                        service.getNode(w, obj.get("nodeId"), PrimitiveNode.class).continueWith(n ->
                            switch (obj.get("subAction").getAsString()) {
                                case "value" -> service.changePrimitiveNodeValue(n, obj.get("value"));
                                default -> ResultOrStringError.error("subAction not recognized");
                            }
                        )
                    );
                case "changeCodeNode" ->
                    service.getWorkflow(obj.get("uuid")).continueWith(w ->
                        service.getNode(w, obj.get("nodeId"), CodeNode.class).continueWith(n ->
                            switch (obj.get("subAction").getAsString()) {
                                case "code" -> service.changeCodeNodeCode(n, obj.get("code"));
                                case "language" -> service.changeCodeNodeLanguage(n, obj.get("language"));
                                default -> ResultOrStringError.error("subAction not recognized");
                            }
                        )
                    );
                case "connect" ->
                    service.getWorkflow(obj.get("uuid")).continueWith(w ->
                        service.getNode(w, obj.get("fromNodeId"), Node.class).continueWith(fromNode ->
                            service.getNode(w, obj.get("toNodeId"), Node.class).continueWith(toNode ->
                                service.getOutputConnector(fromNode, obj.get("fromConnectorId")).continueWith(fromConnector ->
                                    service.getInputConnector(toNode, obj.get("toConnectorId")).continueWith(toConnector ->
                                        service.connect(fromConnector, toConnector)
                                    )
                                )
                            )
                        )
                    );
                case "disconnect" ->
                    service.getWorkflow(obj.get("uuid")).continueWith(w ->
                        service.getNode(w, obj.get("nodeId"), Node.class).continueWith(node ->
                            service.getInputConnector(node, obj.get("connectorId")).continueWith(connector ->
                                service.disconnect(connector)
                            )
                        )
                    );
                default -> ResultOrStringError.error("Action '" + name + "' not supported");
            };
            if (res.getErrorMessage().isPresent()) {
                throw new RuntimeException(res.getErrorMessage().get());
            }
            //If the action was successful and the parameters has an uuid, we start the check for errors
            //We don't want to check every times a move moves if the workflow is valid either (a node movement cam't cause an error)
            if (obj.has("uuid") && !name.equals("moveNode")) {
                service.getWorkflowExecutor(obj.get("uuid")).continueWith(we ->
                    service.checkForErrors(we)
                );
            }
        } catch (Exception e) {
            sendTo(session, errorJson("Error while executing action : " + e.getMessage()));
        }
    }

    private void broadcast(@Nonnull String message) {
        Objects.requireNonNull(message);
        sessions.keySet().forEach(s -> sendTo(s, message));
    }

    /**
     * Sends a message to a specific {@link Session}
     * @param session The session
     * @param message The message
     */
    private void sendTo(@Nonnull Session session, @Nonnull String message) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(message);

        session.getAsyncRemote().sendObject(message, result ->  {
            if (result.getException() != null) {
                System.out.println("Unable to send message: " + result.getException());
            }
        });
    }

    /**
     * Converts the {@link Workflow} to a json representation
     * @param workflow The {@link Workflow}
     * @return The {@link Workflow} as a json representation
     */
    private JsonObject workflowJson(@Nonnull Workflow workflow) {
        Objects.requireNonNull(workflow);

        var obj = new JsonObject();
        obj.addProperty("uuid", workflow.getUUID().toString());
        obj.addProperty("name", workflow.getName());
        return obj;
    }

    /**
     * The base json used to make a notification json
     * @param notificationType The type of the notification
     * @return The base json
     */
    private JsonObject returnJsonObjectBase(@Nonnull String notificationType) {
        Objects.requireNonNull(notificationType);

        var obj = new JsonObject();
        obj.addProperty("notificationType", notificationType);
        return obj;
    }

    /**
     * Converts the error to a json representation
     * @param error The error
     * @return The error as a json representation
     */
    private String errorJson(@Nonnull String error) {
        Objects.requireNonNull(error);

        var toReturn = returnJsonObjectBase("error");
        toReturn.addProperty("error", error);
        return toReturn.toString();
    }

    /**
     * Returns all the existing workflow as json with {@link WorkflowSocket#workflowJson(Workflow)}
     * @return All the existing workflow as json
     */
    private String allWorkflowsJson() {
        var workflows = WorkflowManager.getWorkflowExecutors().stream().map(WorkflowExecutor::getWorkflow).toList();
        var arr = new JsonArray();
        for (var workflow : workflows) {
            arr.add(workflowJson(workflow));
        }

        var toReturn = returnJsonObjectBase("workflows");
        toReturn.add("workflows", arr);

        return toReturn.toString();
    }

    /**
     * Converts the {@link Node} to a json representation
     * @param node The {@link Node}
     * @return The {@link Node} as a json representation
     */
    private String nodeModifiedJson(@Nonnull Node node) {
        Objects.requireNonNull(node);

        var toReturn = returnJsonObjectBase("node");
        toReturn.add("node", node.toJson());

        return toReturn.toString();
    }

    /**
     * Converts the log to a json representation
     * @param log The log
     * @return The log as a json representation
     */
    private String logJson(@Nonnull String log) {
        Objects.requireNonNull(log);

        var toReturn = returnJsonObjectBase("logChanged");
        toReturn.addProperty("log", log);
        return toReturn.toString();
    }

    /**
     * Converts the node removal to a json representation
     * @param node The {@link Node} removed
     * @return The node removal as a json representation
     */
    private String nodeRemovedJson(@Nonnull Node node) {
        Objects.requireNonNull(node);

        var toReturn = returnJsonObjectBase("nodeRemoved");
        toReturn.addProperty("nodeId", node.getId());
        return toReturn.toString();
    }

    /**
     * Converts the {@link Workflow} to a json representation
     * @param workflow The {@link Workflow}
     * @return The {@link Workflow} as a json representation
     */
    private String newWorkflowJson(@Nonnull Workflow workflow) {
        Objects.requireNonNull(workflow);

        var toReturn = returnJsonObjectBase("newWorkflow");
        toReturn.add("workflow", workflowJson(workflow));
        return toReturn.toString();
    }

    /**
     * Converts the workflow to a json representation
     * @param workflow The {@link Workflow} removed
     * @return The workflow removal as a json representation
     */
    private String deletedWorkflowJson(@Nonnull Workflow workflow) {
        Objects.requireNonNull(workflow);

        var toReturn = returnJsonObjectBase("deletedWorkflow");
        toReturn.addProperty("workflowUUID", workflow.getUUID().toString());
        return toReturn.toString();
    }

    /**
     * Converts the {@link NodeState} to a json representation
     * @param state The {@link NodeState}
     * @return The {@link NodeState} as a json representation
     */
    private String nodeStateJson(@Nonnull NodeState state) {
        Objects.requireNonNull(state);

        var toReturn = returnJsonObjectBase("nodeState");
        var ns = new JsonObject();
        ns.addProperty("nodeId", state.getNode().getId());
        ns.addProperty("state", state.getState().toString());
        ns.addProperty("hasBeenModified", state.hasBeenModified());
        ns.addProperty("posX", state.getPosition().x);
        ns.addProperty("posY", state.getPosition().y);
        if (state.getState() == State.FAILED && state.getErrors().isPresent()) {
            var errors = new JsonArray();
            for (var error : state.getErrors().get().getErrors()) {
                errors.add(error.toJson());
            }
            ns.add("execErrors", errors);
        }
        toReturn.add("nodeState", ns);
        return toReturn.toString();
    }

    /**
     * Converts the {@link WorkflowExecutor} state to a json representation
     * @param we The {@link WorkflowExecutor}
     * @return The {@link WorkflowExecutor} state as a json representation
     */
    private String workflowStateJson(@Nonnull WorkflowExecutor we) {
        Objects.requireNonNull(we);

        var toReturn = returnJsonObjectBase("workflowState");
        var ws = new JsonObject();
        ws.addProperty("state", we.getState().toString());
        if (!we.getWorkflowErrors().getErrors().isEmpty()) {
            var errors = new JsonArray();
            for (var error : we.getWorkflowErrors().getErrors()) {
                errors.add(error.toJson());
            }
            ws.add("errors", errors);
        }
        toReturn.add("workflowState", ws);

        return toReturn.toString();
    }

    /**
     * Converts the switch to a new workflow to a json representation
     * @param workflowUUID The workflow UUID
     * @return The switch to a new workflow as a json representation
     */
    private String switchedToJson(String workflowUUID) {
        var toReturn = returnJsonObjectBase("switchedTo");
        toReturn.addProperty("uuid", workflowUUID);
        return toReturn.toString();
    }
}
