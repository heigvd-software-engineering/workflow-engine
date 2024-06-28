package com.heig.resources;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.heig.entities.workflow.NodeModifiedListener;
import com.heig.entities.workflow.execution.State;
import com.heig.entities.workflow.execution.WorkflowExecutionListener;
import com.heig.entities.workflow.nodes.Node;
import com.heig.helpers.ResultOrStringError;
import com.heig.services.WorkflowService;
import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Map;
import java.util.Objects;
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

    private final ConcurrentMap<Session, UUID> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Listener> listeners = new ConcurrentHashMap<>();

    private class Listener implements WorkflowExecutionListener, NodeModifiedListener {
        private final UUID uuid;
        public Listener(@Nonnull UUID uuid) {
            this.uuid = Objects.requireNonNull(uuid);
        }

        private Stream<Session> toNotify() {
            return sessions.entrySet().stream()
                .filter(es -> es.getValue().equals(uuid))
                .map(Map.Entry::getKey);
        }

        @Override
        public void nodeModified(@Nonnull Node node) {

        }

        @Override
        public void workflowStateChanged(@Nonnull State state) {

        }

        @Override
        public void nodeStateChanged(@Nonnull Node node, @Nonnull State state) {

        }

        public void workflowRemoved() {
            //Send a message to tell all clients that this workflow has been removed
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session, null);
        //TODO: Send all the currently available workflows
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
                    //TODO: Broadcast that a new workflow is available
                    yield ResultOrStringError.result(null);
                }
                case "executeWorkflow" ->
                    service.getWorkflowExecutor(obj.get("uuid")).continueWith(we ->
                        service.executeWorkflow(we)
                    );
                case "removeWorkflow" ->
                    service.getWorkflowExecutor(obj.get("uuid")).continueWith(we -> {
                        var listenerToRemove = listeners.remove(we.getWorkflow().getUUID());
                        we.getWorkflow().removeNodeModifiedListener(listenerToRemove);
                        listenerToRemove.workflowRemoved();
                        return service.removeWorkflowExecutor(we);
                        //TODO: Broadcast that the workflow is no longer available
                    });
                case "switchTo" ->
                    service.getWorkflow(obj.get("uuid")).continueWith(w -> {
                        sessions.put(session, null);
                        //TODO: Here send the current state of the new workflow (all nodes, connectors, state, ...)
                        sessions.put(session, w.getUUID());
                        return ResultOrStringError.result(null);
                    });
                default -> ResultOrStringError.error("Action '" + name + "' not supported");
            };
            if (res.getErrorMessage().isPresent()) {
                throw new RuntimeException(res.getErrorMessage().get());
            }
        } catch (Exception e) {
            sendTo(session, "Failed to parse instruction: " + e.getMessage());
        }
    }

    private void broadcast(String message) {
        sessions.keySet().forEach(s -> sendTo(s, message));
    }

    private void sendTo(Session session, String message) {
        session.getAsyncRemote().sendObject(message, result ->  {
            if (result.getException() != null) {
                System.out.println("Unable to send message: " + result.getException());
            }
        });
    }
}
