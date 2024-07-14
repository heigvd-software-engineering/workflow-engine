package com.heig.entities.workflow.data;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WorkflowTypes;
import com.heig.helpers.Utils;
import jakarta.annotation.Nonnull;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Cache {
    private final File cacheDirectory;

    Cache(@Nonnull Workflow w, @Nonnull File rootDirectory) {
        Objects.requireNonNull(w);
        Objects.requireNonNull(rootDirectory);

        cacheDirectory = new File(rootDirectory, "cache");
        if (!cacheDirectory.mkdirs()) {
            throw new RuntimeException("Could not create workflow cache directory");
        }
    }

    private File getNodeCacheDirectory(@Nonnull Node node) {
        return new File(cacheDirectory, String.valueOf(node.getId()));
    }

    private File getOutputConnectorFile(@Nonnull File nodeCacheDirectory, @Nonnull OutputConnector outputConnector, boolean isType) {
        return new File(nodeCacheDirectory, outputConnector.getId() + "." + (isType ? "type" : "obj"));
    }

    private File getInfoFile(@Nonnull File nodeCacheDirectory) {
        return new File(nodeCacheDirectory, ".info");
    }

    private static int getHashFor(@Nonnull Node node, @Nonnull NodeArguments inputs) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(inputs);
        var lstHashCodes = new LinkedList<Integer>();
        for (var input : node.getInputs().values()) {
            var argumentOpt = inputs.getArgument(input.getName());
            if (argumentOpt.isEmpty()) {
                //If the input is optional and is not connected
                if (input.isOptional() && input.getConnectedTo().isEmpty()) {
                    continue;
                }
                throw new RuntimeException("Should never happen ! Node should have been marked as modified !");
            } else {
                var argument = argumentOpt.get();
                lstHashCodes.add(input.getType().getHashCode(argument));
            }
        }
        return Arrays.hashCode(lstHashCodes.toArray());
    }

    public synchronized void set(@Nonnull Node node, @Nonnull NodeArguments inputs, @Nonnull NodeArguments outputs) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(inputs);
        Objects.requireNonNull(outputs);
        var nodeCacheDirectory = getNodeCacheDirectory(node);
        if (nodeCacheDirectory.exists()) {
            Utils.deleteCompleteDirectory(nodeCacheDirectory);
        }
        if (!nodeCacheDirectory.mkdir()) {
            throw new RuntimeException("Could not create cache directory");
        }

        var infoFile = getInfoFile(nodeCacheDirectory);
        try {
            if (!infoFile.createNewFile()) {
                throw new RuntimeException("Could not create info file");
            }
            try (var sw = new BufferedOutputStream(new FileOutputStream(infoFile))) {
                sw.write(ByteBuffer.allocate(4).putInt(getHashFor(node, inputs)).array());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (var output : node.getOutputs().values()) {
            var argumentOpt = outputs.getArgument(output.getName());
            if (argumentOpt.isEmpty()) {
                //All outputs should have a value (even the ones marked as optional -> default type value)
                throw new RuntimeException("Should never happen ! Node should have been marked as modified !");
            }
            var argument = argumentOpt.get();

            var cacheFile = getOutputConnectorFile(nodeCacheDirectory, output, false);
            var cacheFileType = getOutputConnectorFile(nodeCacheDirectory, output, true);
            try {
                if (!cacheFile.createNewFile() || !cacheFileType.createNewFile()) {
                    throw new RuntimeException("Could not create cache file");
                }
                //Here we cannot use output.getType() directly. Imagine the output type is an object but the real
                //type of the argument is a file. We should do the processing for the file type and not the object type
                var argumentType = WorkflowTypes.fromObject(argument);
                Data.toFile(cacheFileType, WorkflowTypes.typeToString(argumentType));
                argumentType.toFile(cacheFile, argument);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized Optional<NodeArguments> get(@Nonnull Node node, @Nonnull NodeArguments currentInputs) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(currentInputs);
        var nodeCacheDirectory = getNodeCacheDirectory(node);
        if (!nodeCacheDirectory.exists() || node.getOutputs().isEmpty()) {
            return Optional.empty();
        }

        var infoFile = getInfoFile(nodeCacheDirectory);
        if (!infoFile.exists()) {
            return Optional.empty();
        }

        int cachedHashCode;
        try (var sr = new BufferedInputStream(new FileInputStream(infoFile))) {
            cachedHashCode = ByteBuffer.wrap(sr.readAllBytes()).getInt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (cachedHashCode != getHashFor(node, currentInputs)) {
            return Optional.empty();
        }

        var nodesArguments = new NodeArguments();
        for (var outputConnector : node.getOutputs().values()) {
            var cacheFile = getOutputConnectorFile(nodeCacheDirectory, outputConnector, false);
            var cacheFileType = getOutputConnectorFile(nodeCacheDirectory, outputConnector, true);
            if (cacheFileType.exists() && cacheFile.exists()) {
                var typeOpt = Data.fromFile(cacheFileType);
                if (typeOpt.isPresent() && typeOpt.get() instanceof String objTypeStr) {
                    var objType = WorkflowTypes.typeFromString(objTypeStr);
                    var obj = objType.fromFile(cacheFile);
                    //Because we checked the hash before, every output that should be available will be
                    obj.ifPresent(o -> nodesArguments.putArgument(outputConnector.getName(), o));
                } else {
                    throw new RuntimeException("Could not find type for the cached value");
                }
            }
        }

        return Optional.of(nodesArguments);
    }

    public synchronized void clear() {
        Utils.deleteCompleteDirectory(cacheDirectory);
    }
}
