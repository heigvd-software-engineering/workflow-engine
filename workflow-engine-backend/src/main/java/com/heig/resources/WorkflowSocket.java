package com.heig.resources;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 *
 * Source: <a href="https://quarkus.io/guides/websockets">Using websockets</a>
 */
@ServerEndpoint("/workflow")
@ApplicationScoped
public class WorkflowSocket {
    Deque<Session> sessions = new ConcurrentLinkedDeque<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        broadcast("User joined");
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        broadcast("User left");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);
        broadcast("User left on error: " + throwable);
    }

    @OnMessage
    public void onMessage(String message) {
        broadcast(">> User: " + message);
    }

    private void broadcast(String message) {
        System.out.println("Broadcast: " + message);
        sessions.forEach(s -> {
            s.getAsyncRemote().sendObject(message, result ->  {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }
}
