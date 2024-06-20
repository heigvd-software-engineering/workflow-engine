package com.heig.entities.workflow.nodes;

import com.heig.testHelpers.TestScenario;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class NodeTest {
    @Test
    public void create() {
        var scenario = new TestScenario();

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

        //The node add should have only one input remaining
        assert scenario.nodeAdd.getInputs().size() == 1;
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

        //The "add" node should have no more outputs connexions
        assert scenario.nodeAdd.getOutputs().isEmpty();
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
}
