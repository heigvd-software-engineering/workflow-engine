package com.heig.testHelpers;

import com.heig.entities.workflow.WorkflowManager;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.nodes.CodeNode;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.nodes.PrimitiveNode;
import com.heig.entities.workflow.types.WObject;
import com.heig.entities.workflow.types.WPrimitive;

import static com.heig.testHelpers.TestUtils.createPrimitiveNode;

public class TestScenario {
    public Workflow w;
    public PrimitiveNode nodeNum1, nodeNum2, nodeStr;
    public CodeNode nodeAdd, nodeStringRepeat;
    public InputConnector num1Input, num2Input, timesInput, stringInput;
    public OutputConnector num1Output, num2Output, strOutput, resultOutput, stringOutput;

    public TestScenario() {
        w = WorkflowManager.createWorkflow("scenario-test");

        //Primitives nodes
        var tnum1 = createPrimitiveNode(w, WPrimitive.Integer);
        var tnum2 = createPrimitiveNode(w, WPrimitive.Integer);
        var tstr = createPrimitiveNode(w, WPrimitive.String);

        num1Output = tnum1.getV1();
        num2Output = tnum2.getV1();
        strOutput = tstr.getV1();

        nodeNum1 = tnum1.getV2();
        nodeNum2 = tnum2.getV2();
        nodeStr = tstr.getV2();

        nodeNum1.setValue(1);
        nodeNum2.setValue(4);
        nodeStr.setValue("Hey ! ");

        //Addition node
        nodeAdd = w.getNodeBuilder().buildCodeNode();
        nodeAdd.setCode(
            """
            let result = arguments.getArgument('num1').get() + arguments.getArgument('num2').get();
            returnArguments.putArgument('result', result);
            """
        );

        num1Input = nodeAdd.getConnectorBuilder().buildInputConnector("num1", WPrimitive.Integer);
        num2Input = nodeAdd.getConnectorBuilder().buildInputConnector("num2", WPrimitive.Integer);

        resultOutput = nodeAdd.getConnectorBuilder().buildOutputConnector("result", WPrimitive.Integer);

        //String repeat node
        nodeStringRepeat = w.getNodeBuilder().buildCodeNode();
        nodeStringRepeat.setCode("arguments.getArgument('')");
        nodeStringRepeat.setCode(
            """
            let times = arguments.getArgument('times').get();
            let str = arguments.getArgument('string').get();
            
            let result = str.repeat(times);
            
            returnArguments.putArgument('string', result);
            """
        );

        timesInput = nodeStringRepeat.getConnectorBuilder().buildInputConnector("times", WPrimitive.Integer);
        stringInput = nodeStringRepeat.getConnectorBuilder().buildInputConnector("string", WPrimitive.String);

        stringOutput = nodeStringRepeat.getConnectorBuilder().buildOutputConnector("string", WPrimitive.String);

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

        var nodeLog = w.getNodeBuilder().buildCodeNode();
        nodeLog.setCode(
            """
            console.log(arguments.getArgument('obj').get());
            """
        );

        var objInput = nodeLog.getConnectorBuilder().buildInputConnector("obj", WObject.of());
        w.connect(stringOutput, objInput);
    }
}