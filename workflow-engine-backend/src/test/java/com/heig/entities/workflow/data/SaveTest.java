package com.heig.entities.workflow.data;

import com.google.gson.Gson;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.execution.*;
import com.heig.testHelpers.TestScenario;
import com.heig.testHelpers.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@QuarkusTest
public class SaveTest {
    @BeforeAll
    public static void init() {
        //Delete everything if there was still a data directory
        Data.clearAll();
    }

    private WorkflowExecutor waitForExecutionEnd(Workflow workflow) throws InterruptedException {
        var currentState = new AtomicReference<State>();
        var executor = WorkflowManager.createWorkflowExecutor(workflow,
            new WorkflowExecutionListener() {
                @Override
                public void workflowStateChanged(@Nonnull WorkflowExecutor we) {
                    currentState.set(we.getState());
                }

                @Override
                public void nodeStateChanged(@Nonnull NodeState state) { }

                @Override
                public void newLogLine(@Nonnull String line) { }

                @Override
                public void clearLog() { }
            }
        );

        assert executor.executeWorkflow();
        while(currentState.get() != State.FINISHED && currentState.get() != State.FAILED) {
            Thread.sleep(50);
        }
        return executor;
    }

    /**
     * Checks if the two workflow executors are the same when serialized
     * @param weBase The workflow executor that was present before the save
     * @param weLoaded The workflow executor that was loaded
     * @return Whether the json of the two are equals
     */
    private static boolean areEquals(WorkflowExecutor weBase, WorkflowExecutor weLoaded) {
        if (weBase == weLoaded) {
            throw new RuntimeException("The two workflow executors are the same");
        }

        var serializer = new WorkflowExecutor.Serializer();
        var gson = new Gson();

        return Objects.equals(gson.toJson(serializer.serialize(weBase)), gson.toJson(serializer.serialize(weLoaded)));
    }

    @Test
    public void empty() throws InterruptedException {
        var w = new Workflow("test");
        var we = TestUtils.createWorkflowExecutor(w);
        var data = Data.getOrCreate(we);
        var save = data.getSave();

        save.save();
        var loaded = save.load(data, new WorkflowExecutionListener() {
            @Override
            public void workflowStateChanged(@Nonnull WorkflowExecutor we) { }

            @Override
            public void nodeStateChanged(@Nonnull NodeState state) { }

            @Override
            public void newLogLine(@Nonnull String line) { }

            @Override
            public void clearLog() { }
        });
        assert loaded.isPresent();

        assert areEquals(we, loaded.get());
    }

    @Test
    public void scenario() throws InterruptedException {
        var scenario = new TestScenario();
        var we = waitForExecutionEnd(scenario.w);
        var data = Data.getOrCreate(we);
        var save = data.getSave();

        save.save();
        var loaded = save.load(data, new WorkflowExecutionListener() {
            @Override
            public void workflowStateChanged(@Nonnull WorkflowExecutor we) { }

            @Override
            public void nodeStateChanged(@Nonnull NodeState state) { }

            @Override
            public void newLogLine(@Nonnull String line) { }

            @Override
            public void clearLog() { }
        });
        assert loaded.isPresent();

        assert areEquals(we, loaded.get());
    }

    @AfterAll
    public static void delete() {
        //Delete everything if there was still a data directory
        Data.clearAll();
    }
}
