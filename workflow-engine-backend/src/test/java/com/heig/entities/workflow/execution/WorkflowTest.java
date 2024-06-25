package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.WorkflowManager;
import com.heig.entities.workflow.types.WObject;
import com.heig.entities.workflow.types.WPrimitive;
import com.heig.testHelpers.TestScenario;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Objects;

@QuarkusTest
public class WorkflowTest {
    @Test
    public void create() {
        var w = WorkflowManager.createWorkflow("exe-create");
        var node1 = w.getNodeBuilder().buildCodeNode();
        Objects.requireNonNull(node1);
        assert node1.getId() >= 0;

        assert w.getNode(node1.getId()).get() == node1;

        var node2 = w.getNodeBuilder().buildCodeNode();
        Objects.requireNonNull(node2);
        assert node2.getId() >= 0 && node1.getId() != node2.getId();

        assert w.getNode(node2.getId()).get() == node2;
    }

    @Test
    public void remove() {
        var w = WorkflowManager.createWorkflow("exe-remove");
        var node1 = w.getNodeBuilder().buildCodeNode();

        w.removeNode(node1);
        assert w.getNode(node1.getId()).isEmpty();

        var node2 = w.getNodeBuilder().buildCodeNode();
        assert node2.getId() != node1.getId();
    }

    @Test
    public void valid() {
        var w = WorkflowManager.createWorkflow("exe-valid");
        //An empty workflow is not valid
        assert w.isValid().isPresent();

        var node1 = w.getNodeBuilder().buildCodeNode();
        //A workflow with only one node is valid
        assert w.isValid().isEmpty();

        var node2 = w.getNodeBuilder().buildCodeNode();
        //A workflow with 2 nodes but no connexions between them is not valid
        assert w.isValid().isPresent();

        //Input type : Object
        //Output type : Object
        var output = node1.getConnectorBuilder().buildOutputConnector("output", WObject.of());
        var input = node2.getConnectorBuilder().buildInputConnector("input", WObject.of());
        w.connect(output, input);
        //After creating a connexion between the two of them, the workflow is valid
        assert w.isValid().isEmpty();

        assert output.setType(WPrimitive.Integer).isEmpty();
        //Input type : Object
        //Output type : Integer
        //=> Valid
        assert w.isValid().isEmpty();

        assert input.setType(WPrimitive.Integer).isEmpty();
        //Input type : Integer
        //Output type : Integer
        //=> Valid
        assert w.isValid().isEmpty();

        assert output.setType(WObject.of()).isEmpty();
        //Input type : Integer
        //Output type : Object
        //=> Not valid
        assert w.isValid().isPresent();

        assert output.setType(WPrimitive.Double).isEmpty();
        //Input type : Integer
        //Output type : Double
        //=> Not valid
        assert w.isValid().isPresent();
    }

    @Test
    public void validScenario() {
        var scenario = new TestScenario();
        assert scenario.w.isValid().isEmpty();
    }
}
