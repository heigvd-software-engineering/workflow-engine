package com.heig.entities.workflow.nodes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.types.WPrimitive;
import com.heig.entities.workflow.types.WorkflowTypes;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.Consumer;

public class PrimitiveNode extends Node {
    public static class Deserializer extends Node.NodeDeserializer<PrimitiveNode> {
        public Deserializer(int id, Workflow workflow) {
            super(id, workflow);
        }

        @Override
        public PrimitiveNode deserialize(JsonElement value) throws JsonParseException {
            var obj = value.getAsJsonObject();

            var outputType = (WPrimitive) WorkflowTypes.typeFromString(obj.get("outputType").getAsString());

            var primitiveNode = new PrimitiveNode(id, workflow, outputType);
            primitiveNode.value = outputType.fromJsonElement(obj.get("value"));

            return primitiveNode;
        }
    }

    public static final String OUTPUT_NAME = "output";
    private final OutputConnector output;

    private Object value;

    protected PrimitiveNode(int id, @Nonnull Workflow workflow, @Nonnull WPrimitive type) {
        super(id, workflow, true);
        Objects.requireNonNull(type);

        output = getConnectorBuilder().buildOutputConnector(OUTPUT_NAME, type);
        setIsDeterministic(true);
        this.value = type.defaultValue();
    }

    @Override
    public NodeArguments execute(@Nonnull NodeArguments inputs, @Nonnull Consumer<String> logLine) {
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
        if (this.value != value) {
            this.value = value;
            getWorkflow().nodeModified(this);
        }
    }

    public OutputConnector getOutputConnector() {
        return output;
    }

    @Override
    public String toString() {
        return "Primitive" + super.toString();
    }

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        var outputType = (WPrimitive) output.getType();
        obj.addProperty("outputType", WorkflowTypes.typeToString(outputType));
        obj.add("value", outputType.toJsonElement(value));
        return obj;
    }
}
