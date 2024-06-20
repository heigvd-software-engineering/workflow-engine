package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.Workflow;
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
        var w = new Workflow();
        var node1 = w.createCodeNode();
        Objects.requireNonNull(node1);
        assert node1.getId() >= 0;

        assert w.getNode(node1.getId()).get() == node1;

        var node2 = w.createCodeNode();
        Objects.requireNonNull(node2);
        assert node2.getId() >= 0 && node1.getId() != node2.getId();

        assert w.getNode(node2.getId()).get() == node2;
    }

    @Test
    public void remove() {
        var w = new Workflow();
        var node1 = w.createCodeNode();

        w.removeNode(node1);
        assert w.getNode(node1.getId()).isEmpty();

        var node2 = w.createCodeNode();
        assert node2.getId() != node1.getId();
    }

    @Test
    public void valid() {
        var w = new Workflow();
        //An empty workflow is not valid
        assert w.isValid().isPresent();

        var node1 = w.createCodeNode();
        //A workflow with only one node is valid
        assert w.isValid().isEmpty();

        var node2 = w.createCodeNode();
        //A workflow with 2 nodes but no connexions between them is not valid
        assert w.isValid().isPresent();

        var output = node1.createOutputConnector("output");
        var input = node2.createInputConnector("input");
        w.connect(output, input);
        //After creating a connexion between the two of them, the workflow is valid
        assert w.isValid().isEmpty();

        output.setType(WPrimitive.Integer);
        //Input type : Object
        //Output type : Integer
        //=> Valid
        assert w.isValid().isEmpty();

        input.setType(WPrimitive.Integer);
        //Input type : Integer
        //Output type : Integer
        //=> Valid
        assert w.isValid().isEmpty();

        output.setType(WObject.of());
        //Input type : Integer
        //Output type : Object
        //=> Not valid
        assert w.isValid().isPresent();

        output.setType(WPrimitive.Double);
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
