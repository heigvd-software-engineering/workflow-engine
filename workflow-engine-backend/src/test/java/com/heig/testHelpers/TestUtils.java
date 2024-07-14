package com.heig.testHelpers;

import com.heig.entities.workflow.data.Cache;
import com.heig.entities.workflow.connectors.InputFlowConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.OutputFlowConnector;
import com.heig.entities.workflow.data.Data;
import com.heig.entities.workflow.execution.*;
import com.heig.entities.workflow.nodes.ModifiableNode;
import com.heig.entities.workflow.nodes.PrimitiveNode;
import com.heig.entities.workflow.types.WFlow;
import com.heig.entities.workflow.types.WPrimitive;
import com.heig.entities.workflow.types.WType;
import groovy.lang.Tuple;
import groovy.lang.Tuple2;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.Objects;

public class TestUtils {
    public static WorkflowExecutor createWorkflowExecutor(String workflowName) {
        var w = new Workflow(workflowName);
        return createWorkflowExecutor(w);
    }

    public static WorkflowExecutor createWorkflowExecutor(Workflow workflow) {
        return WorkflowManager.createWorkflowExecutor(workflow, new WorkflowExecutionListener() {
            @Override
            public void workflowStateChanged(@Nonnull WorkflowExecutor we) { }

            @Override
            public void nodeStateChanged(@Nonnull NodeState state) { }
        });
    }

    public static Tuple2<OutputConnector, PrimitiveNode> createPrimitiveNode(Workflow w, WPrimitive type) {
        var node = w.getNodeBuilder().buildPrimitiveNode(type);

        return Tuple.tuple(node.getOutputConnector(), node);
    }

    public static boolean isCacheValid(Map<Tuple2<String, WType>, Tuple2<Object, Object>> inputs) {
        var we = createWorkflowExecutor("test-cache");
        var w = we.getWorkflow();

        var n = w.getNodeBuilder().buildCodeNode();

        var inputsArgs = new NodeArguments();
        var newInputsArgs = new NodeArguments();
        var outputsArgs = new NodeArguments();

        for (var entry : inputs.entrySet()) {
            var name = entry.getKey().getV1();
            var type = entry.getKey().getV2();
            if (!Objects.equals(name, ModifiableNode.IN_FLOW)) {
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
        outputsArgs.putArgument(ModifiableNode.OUT_FLOW, WFlow.of());

        var cache = Data.getOrCreate(we).getCache();
        cache.set(n, inputsArgs, outputsArgs);
        return cache.get(n, newInputsArgs).isPresent();
    }
}
