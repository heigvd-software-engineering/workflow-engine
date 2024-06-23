package com.heig.entities.workflow.nodes;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.types.WType;
import com.heig.entities.workflow.types.WorkflowTypes;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class PrimitiveNode extends Node {
    public static final String OUTPUT_NAME = "output";
    private final OutputConnector output;

    private Object value;

    protected PrimitiveNode(int id, @Nonnull Workflow workflow, @Nonnull WType type) {
        super(id, workflow, true);
        Objects.requireNonNull(type);

        output = getConnectorBuilder().buildOutputConnector(OUTPUT_NAME, type);
        this.value = type.defaultValue();
    }

    @Nonnull
    @Override
    public NodeArguments execute(@Nonnull NodeArguments arguments) {
        var args = new NodeArguments();
        args.putArgument(OUTPUT_NAME, value);
        return args;
    }

    public void setValue(@Nonnull Object value) {
        var outputType = output.getType();
        var valueType = WorkflowTypes.fromObject(value);

        if (!outputType.canBeConvertedFrom(valueType)) {
            throw new RuntimeException("The value (of type %s) cannot be converted to %s".formatted(valueType, outputType));
        }
        this.value = value;
    }

    public OutputConnector getOutputConnector() {
        return output;
    }
}
