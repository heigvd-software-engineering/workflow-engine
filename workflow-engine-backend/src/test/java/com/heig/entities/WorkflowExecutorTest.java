package com.heig.entities;

import com.heig.testHelpers.TestScenario;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

@QuarkusTest
public class WorkflowExecutorTest {
    @Test
    public void scenario() throws InterruptedException {
        var scenario = new TestScenario();
        var executor = new WorkflowExecutor(scenario.w);
        AtomicReference<State> currentState = new AtomicReference<>();
        executor.executeWorkflow(
            state -> {
                System.out.println("Workflow -> " + state);
                currentState.set(state);
                assert state != State.FAILED;
            }
            ,
            (node, state) -> {
                System.out.println(node.getId() + " -> " + state);
            }
        );
        //While the workflow is running we wait. Only there for tests rightness purpose.
        while(currentState.get() != State.FINISHED) {
            Thread.sleep(10);
        }
    }
}
