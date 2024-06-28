package com.heig.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.heig.entities.workflow.NodeModifiedListener;
import com.heig.entities.workflow.execution.State;
import com.heig.entities.workflow.execution.WorkflowExecutionListener;
import com.heig.entities.workflow.execution.WorkflowManager;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WType;
import com.heig.helpers.ResultOrStringError;
import com.heig.helpers.Utils;
import com.heig.services.WorkflowService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session, null);
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
        var reader = Utils.getJsonReader(message);
        try {
            Utils.readJsonObject(reader, name ->
                Utils.readJsonArray(reader, r -> {
                    ResultOrStringError<Void> res = switch (name) {
                        case "createWorkflow" -> {
                            var wName = r.nextString();
                            var we = service.createWorkflowExecutor(wName, null);
                            we.getWorkflow().addNodeModifiedListener(null);
                            yield ResultOrStringError.result(null);
                        }
                        case "executeWorkflow" -> {
                            var wUUID = r.nextString();
                            yield service.executeWorkflow(wUUID);
                        }
                        case "removeWorkflow" -> {
                            var wUUID = r.nextString();
                            yield service.removeWorkflowExecutor(wUUID);
                        }
                        default -> ResultOrStringError.error("Action '" + name + "' not supported");
                    };
                    if (res.getErrorMessage().isPresent()) {
                        throw new RuntimeException(res.getErrorMessage().get());
                    }
                })
            );
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
