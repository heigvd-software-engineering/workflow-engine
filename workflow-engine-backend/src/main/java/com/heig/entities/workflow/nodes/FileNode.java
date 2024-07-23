package com.heig.entities.workflow.nodes;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.file.FileWrapper;
import com.heig.entities.workflow.types.WFile;
import com.heig.entities.workflow.types.WPrimitive;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

public class FileNode extends Node {
    public static class Deserializer extends Node.NodeDeserializer<FileNode> {
        public Deserializer(int id, Workflow workflow) {
            super(id, workflow);
        }

        @Override
        public FileNode deserialize(JsonElement value) throws JsonParseException {
            return new FileNode(id, workflow);
        }
    }

    public static String I_FILEPATH_NAME = "filePath";
    public static String O_FILE_NAME = "file";

    private final InputConnector input;
    private final OutputConnector output;

    protected FileNode(int id, @Nonnull Workflow workflow) {
        super(id, workflow, true);

        input = getConnectorBuilder().buildInputConnector(I_FILEPATH_NAME, WPrimitive.String);
        output = getConnectorBuilder().buildOutputConnector(O_FILE_NAME, WFile.of());
    }

    @Override
    public NodeArguments execute(@Nonnull NodeArguments inputs, @Nonnull Consumer<String> logLine) {
        var filePathOpt = inputs.getArgument(input.getName());
        if (filePathOpt.isEmpty()) {
            throw new RuntimeException("No file path specified");
        }
        var filePathObj = filePathOpt.get();
        if (!(filePathObj instanceof String filePath)) {
            throw new RuntimeException("The filePath argument is not a string");
        }

        var returnArgs = new NodeArguments();
        returnArgs.putArgument(output.getName(), new FileWrapper(filePath));
        return returnArgs;
    }
}
