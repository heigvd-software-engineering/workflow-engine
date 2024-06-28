package com.heig.resources;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.websocket.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 *
 * Source: <a href="https://quarkus.io/guides/websockets">Using websockets</a>
 */
@QuarkusTest
public class WorkflowSocketTest {
    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/workflow")
    URI uri;

    @Test
    public void testWebsocketWorkflow() throws Exception {
//        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri)) {
//            Assertions.assertEquals("CONNECT", MESSAGES.poll(10, TimeUnit.SECONDS));
//            Assertions.assertEquals("User joined", MESSAGES.poll(10, TimeUnit.SECONDS));
//            session.getAsyncRemote().sendText("hello world");
//            Assertions.assertEquals(">> User: hello world", MESSAGES.poll(10, TimeUnit.SECONDS));
//            session.close();
//            Assertions.assertEquals("CLOSE", MESSAGES.poll(10, TimeUnit.SECONDS));
//            Assertions.assertTrue(MESSAGES.isEmpty());
//        }
    }

    @ClientEndpoint
    public static class Client {
        @OnOpen
        void onOpen(Session session) {
            MESSAGES.add("CONNECT");
        }

        @OnMessage
        void onMessage(String msg) {
            MESSAGES.add(msg);
        }

        @OnClose
        void onClose(Session session) {
            MESSAGES.add("CLOSE");
        }
    }
}
