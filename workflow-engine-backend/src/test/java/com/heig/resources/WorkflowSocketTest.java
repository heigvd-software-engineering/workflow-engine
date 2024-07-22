package com.heig.resources;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.heig.entities.workflow.data.Data;
import com.heig.entities.workflow.execution.WorkflowManager;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.websocket.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 *
 * Source: <a href="https://quarkus.io/guides/websockets">Using websockets</a>
 */
@QuarkusTest
public class WorkflowSocketTest {
    @BeforeAll
    public static void setup() {
        Data.clearAll();
    }

    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/workflow")
    URI uri;

    @Test
    public void testWebsocketWorkflow() throws Exception {
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri)) {
            Assertions.assertEquals("CONNECT", MESSAGES.poll(1, TimeUnit.SECONDS));
            
            //Receive a notification with all the workflows
            var workflowJson = MESSAGES.poll(1, TimeUnit.SECONDS);
            var gson = new Gson();
            var obj = gson.fromJson(workflowJson, JsonObject.class);
            assert Objects.equals(obj.get("notificationType").getAsString(), "workflows");
            var arr = obj.get("workflows").getAsJsonArray();
            //We should have the same number of workflow as in the WorkflowManager
            assert arr.asList().size() == WorkflowManager.getWorkflowExecutors().size();

            session.close();
            Assertions.assertEquals("CLOSE", MESSAGES.poll(1, TimeUnit.SECONDS));
            Assertions.assertTrue(MESSAGES.isEmpty());
        }
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
