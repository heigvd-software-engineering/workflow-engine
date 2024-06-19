package com.heig.entities;

import com.heig.entities.workflowTypes.WPrimitive;
import com.heig.entities.workflowTypes.WPrimitiveTypes;
import com.heig.testHelpers.TestScenario;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Objects;

@QuarkusTest
public class WorkflowTest {
    @Test
    public void create() {
        var w = new Workflow();
        var node1 = w.createNode();
        Objects.requireNonNull(node1);
        assert node1.getId() >= 0;

        assert w.getNode(node1.getId()).get() == node1;

        var node2 = w.createNode();
        Objects.requireNonNull(node2);
        assert node2.getId() >= 0 && node1.getId() != node2.getId();

        assert w.getNode(node2.getId()).get() == node2;
    }

    @Test
    public void remove() {
        var w = new Workflow();
        var node1 = w.createNode();

        w.removeNode(node1);
        assert w.getNode(node1.getId()).isEmpty();

        var node2 = w.createNode();
        assert node2.getId() != node1.getId();
    }

    @Test
    public void valid() {
        var w = new Workflow();
        //An empty workflow is not valid
        assert w.isValid().isPresent();

        var node1 = w.createNode();
        //A workflow with only one node is valid
        assert w.isValid().isEmpty();

        var node2 = w.createNode();
        //A workflow with 2 nodes but no connexions between them is not valid
        assert w.isValid().isPresent();

        var output = node1.createOutputConnector("output");
        var input = node2.createInputConnector("input");
        w.connect(output, input);
        //After creating a connexion between the two of them, the workflow is valid
        assert w.isValid().isEmpty();

        output.setType(WPrimitive.of(WPrimitiveTypes.Integer));
        //Input type : Object
        //Output type : Integer
        //=> Valid
        assert w.isValid().isEmpty();

        input.setType(WPrimitive.of(WPrimitiveTypes.Integer));
        //Input type : Integer
        //Output type : Integer
        //=> Valid
        assert w.isValid().isEmpty();

        //Input type : Double
        //Output type : Integer
        //=> Not valid
        input.setType(WPrimitive.of(WPrimitiveTypes.Double));
        assert w.isValid().isPresent();
    }

    @Test
    public void validScenario() {
        var scenario = new TestScenario();
        assert scenario.w.isValid().isEmpty();
    }
}
