package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.nodes.Node;
import com.heig.testHelpers.TestScenario;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

@QuarkusTest
public class WorkflowExecutorTest {
    @Test
    public void scenario() throws InterruptedException {
        var scenario = new TestScenario();
        var currentState = new AtomicReference<State>();
        var executor = new WorkflowExecutor(scenario.w,
            new WorkflowExecutionListener() {
                @Override
                public void workflowStateChanged(@Nonnull WorkflowExecutor we) {
                    System.out.println("Workflow -> " + we.getState());
                    currentState.set(we.getState());
                }

                @Override
                public void nodeStateChanged(@Nonnull NodeState state) {
                    System.out.println(state.getNode().getId() + " -> " + state.getState());
                }
            }
        );
        assert executor.executeWorkflow();
        //While the workflow is running we wait. Only there for tests rightness purpose.
        while(currentState.get() != State.FINISHED && currentState.get() != State.FAILED) {
            Thread.sleep(50);
        }
        assert currentState.get() != State.FAILED;

        //After the workflow execution, we clear the cache
        executor.clearCache();
    }
}
