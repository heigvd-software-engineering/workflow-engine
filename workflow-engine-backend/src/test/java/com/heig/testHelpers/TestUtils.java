package com.heig.testHelpers;

import com.heig.entities.workflow.data.Cache;
import com.heig.entities.workflow.connectors.InputFlowConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.OutputFlowConnector;
import com.heig.entities.workflow.data.Data;
import com.heig.entities.workflow.execution.*;
import com.heig.entities.workflow.file.FileWrapper;
import com.heig.entities.workflow.nodes.FileNode;
import com.heig.entities.workflow.nodes.ModifiableNode;
import com.heig.entities.workflow.nodes.PrimitiveNode;
import com.heig.entities.workflow.types.*;
import groovy.lang.Tuple;
import groovy.lang.Tuple2;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

            @Override
            public void newLogLine(@Nonnull String line) { }

            @Override
            public void clearLog() { }
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
            n.getConnectorBuilder().buildInputConnector(name, type);
            var inputValue = entry.getValue().getV1();
            if (inputValue != null) {
                inputsArgs.putArgument(name, entry.getValue().getV1());
            }
            var newInputValue = entry.getValue().getV2();
            if (newInputValue != null) {
                newInputsArgs.putArgument(name, entry.getValue().getV2());
            }
        }

        var cache = Data.getOrCreate(we).getCache();
        cache.set(n, inputsArgs, outputsArgs);
        return cache.get(n, newInputsArgs).isPresent();
    }

    public static Optional<FileWrapper> executeFileNode(FileNode node, String path) {
        var nodeArgs = new NodeArguments();
        nodeArgs.putArgument(FileNode.I_FILEPATH_NAME, path);
        var resArgs = node.execute(nodeArgs, (str) -> { });
        return resArgs.getArgument(FileNode.O_FILE_NAME).map(o -> o instanceof FileWrapper ? (FileWrapper) o : null);
    }

    public static boolean isFileCacheValid(Consumer<FileWrapper> beforeSet, Consumer<FileWrapper> betweenSetAndGet, Function<FileWrapper, Boolean> validity) {
        var path = "file.test";
        var inputsArgs = new NodeArguments();
        inputsArgs.putArgument(FileNode.I_FILEPATH_NAME, path);

        var w = new Workflow("file-node");
        var fileNode = w.getNodeBuilder().buildFileNode();

        var outputArgs = new NodeArguments();
        var fwOpt = executeFileNode(fileNode, path);
        if (fwOpt.isEmpty()) {
            return false;
        }
        var fw = fwOpt.get();
        outputArgs.putArgument(FileNode.O_FILE_NAME, fw);

        var we = TestUtils.createWorkflowExecutor(w);
        var cache = Data.getOrCreate(we).getCache();

        beforeSet.accept(fw);
        cache.set(fileNode, inputsArgs, outputArgs);
        betweenSetAndGet.accept(fw);
        var isValid = cache.get(fileNode, inputsArgs).isPresent() && validity.apply(fw);
        fw.delete();
        return isValid;
    }

    public static boolean hashCodeTest(Supplier<Object> supp) {
        var realType = WorkflowTypes.fromObject(supp.get());
        return realType.getHashCode(supp.get()) == realType.getHashCode(supp.get());
    }
}
