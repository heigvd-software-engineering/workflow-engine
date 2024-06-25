package com.heig.entities.workflow.execution;

import com.heig.testHelpers.TestScenario;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

@QuarkusTest
public class WorkflowExecutorTest {
    @Test
    public void scenario() throws InterruptedException {
        var scenario = new TestScenario();
        var currentState = new AtomicReference<State>();
        var executor = new WorkflowExecutor(scenario.w,
            state -> {
                System.out.println("Workflow -> " + state);
                currentState.set(state);
            }
            ,
            (node, state) -> {
                System.out.println(node.getId() + " -> " + state);
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
