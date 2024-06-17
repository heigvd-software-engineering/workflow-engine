package com.heig.entities;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Function;

@QuarkusTest
public class NodeTest {
    private static Node createPrimitiveNode(WorkflowTypes type) {
        var output = new OutputConnector();
        output.setType(type);

        var node = new Node();
        node.addOutput(output);

        return node;
    }

    @Test
    public void create() {
        //Primitives nodes
        var num1 = createPrimitiveNode(WorkflowTypes.Integer);
        var num2 = createPrimitiveNode(WorkflowTypes.Integer);
        var str = createPrimitiveNode(WorkflowTypes.String);

        //Addition node
        var num1Input = new InputConnector();
        num1Input.setType(WorkflowTypes.Integer);
        var num2Input = new InputConnector();
        num1Input.setType(WorkflowTypes.Integer);

        var resultOutput = new OutputConnector();
        resultOutput.setType(WorkflowTypes.Integer);

        var nodeAdd = new Node();
        assert nodeAdd.addInput(num1Input);
        assert nodeAdd.addInput(num2Input);

        assert nodeAdd.addOutput(resultOutput);

        //String repeat node
        var timesInput = new InputConnector();
        timesInput.setType(WorkflowTypes.Integer);
        var stringInput = new InputConnector();
        stringInput.setType(WorkflowTypes.String);

        var stringOutput = new OutputConnector();
        stringOutput.setType(WorkflowTypes.String);

        var nodeStringRepeat = new Node();
        assert nodeStringRepeat.addInput(timesInput);
        assert nodeStringRepeat.addInput(stringInput);

        assert nodeStringRepeat.addOutput(stringOutput);

        /* Connect nodes
            ┌─num1─┐   ┌───────add──────┐
            │     o├───►o num1          │
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
        assert Node.connect(num1.getOutputs().get(0), num1Input);
        assert Node.connect(num2.getOutputs().get(0), num2Input);

        assert Node.connect(resultOutput, timesInput);
        assert Node.connect(str.getOutputs().get(0), stringInput);

        //We verify that the "add" node is connected to the "stringRepeat" node
        assert timesInput.getConnectedTo().get().equals(resultOutput);
        //and that the node "add" is connected to "stringRepeat"
        assert resultOutput.getConnectedTo().contains(timesInput);

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
        assert Node.connect(num1.getOutputs().get(0), timesInput);

        //We verify that the "num1" node is connected to the "stringRepeat" node
        assert timesInput.getConnectedTo().get().equals(num1.getOutputs().get(0));
        //and that the node "add" is not connected to "stringRepeat" anymore
        assert resultOutput.getConnectedTo().isEmpty();
    }

    @Test
    public void remove() {
        //Primitives nodes
        var num1 = createPrimitiveNode(WorkflowTypes.Integer);
        var num2 = createPrimitiveNode(WorkflowTypes.Integer);
        var str = createPrimitiveNode(WorkflowTypes.String);

        //Addition node
        var num1Input = new InputConnector();
        num1Input.setType(WorkflowTypes.Integer);
        var num2Input = new InputConnector();
        num1Input.setType(WorkflowTypes.Integer);

        var resultOutput = new OutputConnector();
        resultOutput.setType(WorkflowTypes.Integer);

        var nodeAdd = new Node();
        assert nodeAdd.addInput(num1Input);
        assert nodeAdd.addInput(num2Input);

        assert nodeAdd.addOutput(resultOutput);

        //String repeat node
        var timesInput = new InputConnector();
        timesInput.setType(WorkflowTypes.Integer);
        var stringInput = new InputConnector();
        stringInput.setType(WorkflowTypes.String);

        var stringOutput = new OutputConnector();
        stringOutput.setType(WorkflowTypes.String);

        var nodeStringRepeat = new Node();
        assert nodeStringRepeat.addInput(timesInput);
        assert nodeStringRepeat.addInput(stringInput);

        assert nodeStringRepeat.addOutput(stringOutput);

        /* Connect nodes
            ┌─num1─┐   ┌───────add──────┐
            │     o├───►o num1          │
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
        assert Node.connect(num1.getOutputs().get(0), num1Input);
        assert Node.connect(num2.getOutputs().get(0), num2Input);

        assert Node.connect(resultOutput, timesInput);
        assert Node.connect(str.getOutputs().get(0), stringInput);

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
        assert nodeAdd.removeInput(num1Input);

        //The node add should have only one input remaining
        assert nodeAdd.getInputs().size() == 1;
        //The output connexion of the "num1" node should be connected to no one
        assert num1.getOutputs().get(0).getConnectedTo().isEmpty();

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
        assert nodeAdd.removeOutput(resultOutput);

        //The "add" node should have no more outputs connexions
        assert nodeAdd.getOutputs().isEmpty();
        //The "times" connexion of the "stringRepeat" node should have no connexion
        assert timesInput.getConnectedTo().isEmpty();
    }
}
