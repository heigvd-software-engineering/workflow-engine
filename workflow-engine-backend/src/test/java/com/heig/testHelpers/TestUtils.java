package com.heig.testHelpers;

import com.heig.entities.workflow.WorkflowManager;
import com.heig.entities.workflow.cache.Cache;
import com.heig.entities.workflow.connectors.InputFlowConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.OutputFlowConnector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.nodes.PrimitiveNode;
import com.heig.entities.workflow.types.WFlow;
import com.heig.entities.workflow.types.WPrimitive;
import com.heig.entities.workflow.types.WType;
import com.heig.entities.workflow.types.WorkflowTypes;
import groovy.lang.Tuple;
import groovy.lang.Tuple2;
import groovy.lang.Tuple3;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TestUtils {
    public static Tuple2<OutputConnector, PrimitiveNode> createPrimitiveNode(Workflow w, WPrimitive type) {
        var node = w.getNodeBuilder().buildPrimitiveNode(type);

        return Tuple.tuple(node.getOutputConnector(), node);
    }

    public static boolean isCacheValid(Map<Tuple2<String, WType>, Tuple2<Object, Object>> inputs) {
        var w = WorkflowManager.createWorkflow("test-cache");

        var n = w.getNodeBuilder().buildCodeNode();

        var inputsArgs = new NodeArguments();
        var newInputsArgs = new NodeArguments();
        var outputsArgs = new NodeArguments();

        for (var entry : inputs.entrySet()) {
            var name = entry.getKey().getV1();
            var type = entry.getKey().getV2();
            if (!Objects.equals(name, InputFlowConnector.CONNECTOR_NAME)) {
                n.getConnectorBuilder().buildInputConnector(name, type);
            }
            var inputValue = entry.getValue().getV1();
            if (inputValue != null) {
                inputsArgs.putArgument(name, entry.getValue().getV1());
            }
            var newInputValue = entry.getValue().getV2();
            if (newInputValue != null) {
                newInputsArgs.putArgument(name, entry.getValue().getV2());
            }
        }
        outputsArgs.putArgument(OutputFlowConnector.CONNECTOR_NAME, WFlow.of());

        var cache = Cache.get(w);
        cache.set(n, inputsArgs, outputsArgs);
        return cache.get(n, newInputsArgs).isPresent();
    }
}
