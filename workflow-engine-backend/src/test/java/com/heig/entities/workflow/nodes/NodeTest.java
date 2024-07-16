package com.heig.entities.workflow.nodes;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.data.Data;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.execution.WorkflowManager;
import com.heig.entities.workflow.connectors.InputFlowConnector;
import com.heig.entities.workflow.connectors.OutputFlowConnector;
import com.heig.entities.workflow.types.WFile;
import com.heig.entities.workflow.types.WFlow;
import com.heig.entities.workflow.types.WPrimitive;
import com.heig.testHelpers.TestScenario;
import com.heig.testHelpers.TestUtils;
import groovy.lang.Tuple2;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

@QuarkusTest
public class NodeTest {


    @Test
    public void create() {
        var scenario = new TestScenario();

        var connectorBuilder = scenario.nodeAdd.getConnectorBuilder();
        connectorBuilder.buildInputFlowConnector("in-flow");
        connectorBuilder.buildOutputFlowConnector("out-flow");

        //Adding another flow connector should throw because the name exists already
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            scenario.nodeAdd.getConnectorBuilder().buildInputFlowConnector("in-flow");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            scenario.nodeAdd.getConnectorBuilder().buildOutputFlowConnector("out-flow");
        });

        /* We now directly link the "num1" node to the "stringRepeat" node
            ┌─num1─┐
            │     o├─┬─────────────────────┐
            └──────┘ │                     │
                     │ ┌───────add──────┐  │
                     └─►o num1          │  │
                       │                │  │ ┌─stringRepeat─┐
                       │        result o│  └─►o times       │
            ┌─num2─┐   │                │    │              │
            │     o├───►o num2          │    │      string o│
            └──────┘   └────────────────┘    │              │
                                           ┌─►o string      │
                                           │ └──────────────┘
                                  ┌─str─┐  │
                                  │    o├──┘
                                  └─────┘
        */
        assert scenario.w.connect(scenario.num1Output, scenario.timesInput);

        //We verify that the "num1" node is connected to the "stringRepeat" node
        assert scenario.timesInput.getConnectedTo().get().equals(scenario.num1Output);
        //and that the node "add" is not connected to "stringRepeat" anymore
        assert scenario.resultOutput.getConnectedTo().isEmpty();
    }

    @Test
    public void remove() {
        var scenario = new TestScenario();

        /* Removing "num1" input connexion of the "add" node
            ┌─num1─┐   ┌───────add──────┐
            │     o│   │                │
            └──────┘   │                │    ┌─stringRepeat─┐
                       │        result o├────►o times       │
            ┌─num2─┐   │                │    │              │
            │     o├───►o num2          │    │      string o│
            └──────┘   └────────────────┘    │              │
                                           ┌─►o string      │
                                           │ └──────────────┘
                                  ┌─str─┐  │
                                  │    o├──┘
                                  └─────┘
        */
        assert scenario.nodeAdd.removeInput(scenario.num1Input);

        //The node "add" should not have the "num1" input anymore
        assert !scenario.nodeAdd.getInputs().containsKey(scenario.num1Input.getId());
        //The output connexion of the "num1" node should be connected to no one
        assert scenario.num1Output.getConnectedTo().isEmpty();

        /* Removing "result" output connexion of the "add" node
            ┌─num1─┐   ┌───────add──────┐
            │     o│   │                │
            └──────┘   │                │    ┌─stringRepeat─┐
                       │                │    │o times       │
            ┌─num2─┐   │                │    │              │
            │     o├───►o num2          │    │      string o│
            └──────┘   └────────────────┘    │              │
                                           ┌─►o string      │
                                           │ └──────────────┘
                                  ┌─str─┐  │
                                  │    o├──┘
                                  └─────┘
        */
        assert scenario.nodeAdd.removeOutput(scenario.resultOutput);

        //The "add" node should not have the "result" output anymore
        assert !scenario.nodeAdd.getOutputs().containsKey(scenario.resultOutput.getId());
        //The "times" connexion of the "stringRepeat" node should have no connexion
        assert scenario.timesInput.getConnectedTo().isEmpty();
    }

    @Test
    public void removeNode() {
        var scenario = new TestScenario();

        /* Removing the "add" node
            ┌─num1─┐
            │     o│
            └──────┘                         ┌─stringRepeat─┐
                                             │o times       │
            ┌─num2─┐                         │              │
            │     o│                         │      string o│
            └──────┘                         │              │
                                           ┌─►o string      │
                                           │ └──────────────┘
                                  ┌─str─┐  │
                                  │    o├──┘
                                  └─────┘
         */
        assert scenario.w.removeNode(scenario.nodeAdd);

        //The "num1" and "num2" node should not be connected to anything anymore
        assert scenario.num1Output.getConnectedTo().isEmpty();
        assert scenario.num2Output.getConnectedTo().isEmpty();

        //Same for the "times" connexion of the "stringRepeat" node
        assert scenario.timesInput.getConnectedTo().isEmpty();

        //The "add" node should not be connected to anything anymore ("num1" and "num2" inputs and "result" output)
        assert scenario.num1Input.getConnectedTo().isEmpty();
        assert scenario.num2Input.getConnectedTo().isEmpty();
        assert scenario.resultOutput.getConnectedTo().isEmpty();
    }

    @Test
    public void readOnlyConnector() {
        var w = new Workflow("ro-connector");
        var n = w.getNodeBuilder().buildPrimitiveNode(WPrimitive.Integer);

        //The connector should be marked as readonly
        var outputConn = n.getOutputs().values().stream().filter(c -> c.getName().equals(PrimitiveNode.OUTPUT_NAME)).findFirst();
        assert outputConn.isPresent();
        assert outputConn.get().isReadOnly();
    }

    @Test
    public void flowConnector() {
        var w = new Workflow("flow-connector");
        var n = w.getNodeBuilder().buildCodeNode();
        n.getConnectorBuilder().buildOutputFlowConnector("flow");

        //The connector should be marked as optional
        var outputConn = n.getOutputs().values().stream().filter(c -> c.getName().equals("flow")).findFirst();
        assert outputConn.isPresent();
        assert outputConn.get().isOptional();
    }
}
