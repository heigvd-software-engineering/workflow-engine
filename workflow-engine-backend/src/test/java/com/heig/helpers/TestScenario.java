package com.heig.helpers;

import com.heig.entities.*;
import com.heig.entities.workflowTypes.WPrimitive;
import com.heig.entities.workflowTypes.WPrimitiveTypes;

import static com.heig.helpers.TestUtils.createPrimitiveNode;

public class TestScenario {
    public Workflow w;
    public Node nodeNum1, nodeNum2, nodeStr, nodeAdd, nodeStringRepeat;
    public InputConnector num1Input, num2Input, timesInput, stringInput;
    public OutputConnector num1Output, num2Output, strOutput, resultOutput, stringOutput;

    public TestScenario() {
        w = new Workflow();

        //Primitives nodes
        var tnum1 = createPrimitiveNode(w, WPrimitive.of(WPrimitiveTypes.Integer));
        var tnum2 = createPrimitiveNode(w, WPrimitive.of(WPrimitiveTypes.Integer));
        var tstr = createPrimitiveNode(w, WPrimitive.of(WPrimitiveTypes.String));

        num1Output = tnum1.getV1();
        num2Output = tnum2.getV1();
        strOutput = tstr.getV1();

        nodeNum1 = tnum1.getV2();
        nodeNum2 = tnum2.getV2();
        nodeStr = tstr.getV2();

        //Addition node
        nodeAdd = w.createNode();

        num1Input = nodeAdd.createInputConnector();
        num1Input.setType(WPrimitive.of(WPrimitiveTypes.Integer));
        num2Input = nodeAdd.createInputConnector();
        num2Input.setType(WPrimitive.of(WPrimitiveTypes.Integer));

        resultOutput = nodeAdd.createOutputConnector();
        resultOutput.setType(WPrimitive.of(WPrimitiveTypes.Integer));

        //String repeat node
        nodeStringRepeat = w.createNode();

        timesInput = nodeStringRepeat.createInputConnector();
        timesInput.setType(WPrimitive.of(WPrimitiveTypes.Integer));
        stringInput = nodeStringRepeat.createInputConnector();
        stringInput.setType(WPrimitive.of(WPrimitiveTypes.String));

        stringOutput = nodeStringRepeat.createOutputConnector();
        stringOutput.setType(WPrimitive.of(WPrimitiveTypes.String));

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
        assert w.connect(tnum1.getV1(), num1Input);
        assert w.connect(tnum2.getV1(), num2Input);

        assert w.connect(resultOutput, timesInput);
        assert w.connect(tstr.getV1(), stringInput);

        //We verify that the "add" node is connected to the "stringRepeat" node
        assert timesInput.getConnectedTo().get().equals(resultOutput);
        //and that the node "add" is connected to "stringRepeat"
        assert resultOutput.getConnectedTo().contains(timesInput);
    }
}