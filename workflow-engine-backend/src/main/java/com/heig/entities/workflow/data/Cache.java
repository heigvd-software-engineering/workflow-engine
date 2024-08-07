package com.heig.entities.workflow.data;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.types.WorkflowTypes;
import com.heig.helpers.Utils;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Represents the cache for a workflow
 */
public class Cache {
    /**
     * The cache directory
     */
    private final File cacheDirectory;

    Cache(@Nonnull Workflow w, @Nonnull File rootDirectory) {
        Objects.requireNonNull(w);
        Objects.requireNonNull(rootDirectory);

        cacheDirectory = new File(rootDirectory, "cache");
        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) {
            throw new RuntimeException("Could not create workflow cache directory");
        }
    }

    /**
     * Returns the cache directory for the node
     * @param node The node
     * @return The cache directory
     */
    private File getNodeCacheDirectory(@Nonnull Node node) {
        return new File(cacheDirectory, String.valueOf(node.getId()));
    }

    /**
     * Returns the cache file for an input connector
     * @param nodeCacheDirectory The cache directory of the node
     * @param outputConnector The output connector
     * @param isType If true, returns a file ending in .type, .obj otherwise
     * @return The cache file for the connector
     */
    private File getOutputConnectorFile(@Nonnull File nodeCacheDirectory, @Nonnull OutputConnector outputConnector, boolean isType) {
        return new File(nodeCacheDirectory, outputConnector.getId() + "." + (isType ? "type" : "obj"));
    }

    /**
     * Return the info file for the node cache directory
     * @param nodeCacheDirectory The node cache directory
     * @return The info file for the node cache directory
     */
    private File getInfoFile(@Nonnull File nodeCacheDirectory) {
        return new File(nodeCacheDirectory, ".info");
    }

    /**
     * Returns the hash for all the inputs
     * @param node The node
     * @param inputs The inputs
     * @return The hash
     */
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
                //Here we use the real type, not the one declared by the input.
                //If the input was WObject, but the type is WFile, we want to get the hash code from the WFile type, not from WObject
                var realType = WorkflowTypes.fromObject(argument);
                lstHashCodes.add(realType.getHashCode(argument));
            }
        }
        return Arrays.hashCode(lstHashCodes.toArray());
    }

    /**
     * Sets the cache for the node
     * @param node The node
     * @param inputs The inputs
     * @param outputs The outputs
     */
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
                if (!cacheFileType.createNewFile()) {
                    throw new RuntimeException("Could not create cache file type");
                }
                //Here we cannot use output.getType() directly. Imagine the output type is an object but the real type of the value is a file.
                //We should do the processing for the file type and not the object type
                var argumentType = WorkflowTypes.fromObject(argument);
                Data.toFile(cacheFileType, WorkflowTypes.typeToString(argumentType));
                argumentType.toFile(cacheFile, argument);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Tries to get the outputs from the cache for a node
     * @param node The node
     * @param currentInputs The current inputs
     * @return The outputs or {@link Optional#empty()} if the retrieval failed (the hash code of the inputs changed for example)
     */
    public synchronized Optional<NodeArguments> get(@Nonnull Node node, @Nonnull NodeArguments currentInputs) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(currentInputs);
        var nodeCacheDirectory = getNodeCacheDirectory(node);
        if (!nodeCacheDirectory.exists()) {
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

        //The hash code stored in the file should be equals to the hash of the currentInputs
        if (cachedHashCode != getHashFor(node, currentInputs)) {
            return Optional.empty();
        }

        var nodesArguments = new NodeArguments();
        for (var outputConnector : node.getOutputs().values()) {
            var cacheFile = getOutputConnectorFile(nodeCacheDirectory, outputConnector, false);
            var cacheFileType = getOutputConnectorFile(nodeCacheDirectory, outputConnector, true);
            if (cacheFileType.exists()) {
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

    /**
     * Clears the cache for this workflow
     */
    public synchronized void clear() {
        Utils.deleteCompleteDirectory(cacheDirectory);
    }
}
