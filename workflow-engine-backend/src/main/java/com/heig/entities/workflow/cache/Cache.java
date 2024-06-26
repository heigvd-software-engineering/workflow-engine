package com.heig.entities.workflow.cache;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.nodes.Node;
import com.heig.helpers.Utils;
import io.quarkus.runtime.util.HashUtil;
import io.smallrye.common.annotation.CheckReturnValue;
import jakarta.annotation.Nonnull;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class Cache {
    private static final ConcurrentMap<UUID, Cache> instances = new ConcurrentHashMap<>();

    private static final File cacheRootDirectory = new File(ConfigProvider.getConfig().getValue("cache_directory", String.class));
    private final File cacheDirectory;

    private Cache(@Nonnull Workflow w) {
        Objects.requireNonNull(w);

        cacheDirectory = new File(cacheRootDirectory, w.getUuid().toString());
        clear();
        if (!cacheDirectory.mkdirs()) {
            throw new RuntimeException("Could not create workflow cache directory");
        }
    }

    public static Cache get(@Nonnull Workflow w) {
        return instances.computeIfAbsent(w.getUuid(), (uuid) -> new Cache(w));
    }

    private File getNodeCacheDirectory(@Nonnull Node node) {
        return new File(cacheDirectory, String.valueOf(node.getId()));
    }

    private File getOutputConnectorFile(@Nonnull File nodeCacheDirectory, @Nonnull OutputConnector outputConnector) {
        return new File(nodeCacheDirectory, outputConnector.getId() + ".obj");
    }

    private File getInfoFile(@Nonnull File nodeCacheDirectory) {
        return new File(nodeCacheDirectory, ".info");
    }

    private static int getHashFor(@Nonnull Node node, @Nonnull NodeArguments inputs) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(inputs);
//        for (var argumentName : inputs.getArguments().keySet()) {
//            var inputConnectorOpt = node.getInputs().values().stream()
//                    .filter(o -> o.getName().equals(argumentName))
//                    .findFirst();
//            if (inputConnectorOpt.isEmpty()) {
//                continue;
//            }
//            var inputConnector = inputConnectorOpt.get();
//
//            var argument = inputs.getArguments().get(argumentName);
//            lstHashCodes.add(inputConnector.getType().getHashCode(argument));
//        }
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

    @CheckReturnValue
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

//        for (var argumentName : outputs.getArguments().keySet()) {
//            var outputConnectorOpt = node.getOutputs().values().stream()
//                    .filter(o -> o.getName().equals(argumentName))
//                    .findFirst();
//            if (outputConnectorOpt.isEmpty()) {
//                continue;
//            }
//            var outputConnector = outputConnectorOpt.get();
//
//            var argument = outputs.getArguments().get(argumentName);
//
//            var cacheFile = getOutputConnectorFile(nodeCacheDirectory, outputConnector);
//            try {
//                if (!cacheFile.createNewFile()) {
//                    throw new RuntimeException("Could not create cache file");
//                }
//                outputConnector.getType().toFile(cacheFile, argument);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }

        for (var output : node.getOutputs().values()) {
            var argumentOpt = outputs.getArgument(output.getName());
            if (argumentOpt.isEmpty()) {
                //All outputs should have a value (even the ones marked as optional -> default type value)
                throw new RuntimeException("Should never happen ! Node should have been marked as modified !");
            }
            var argument = argumentOpt.get();

            var cacheFile = getOutputConnectorFile(nodeCacheDirectory, output);
            try {
                if (!cacheFile.createNewFile()) {
                    throw new RuntimeException("Could not create cache file");
                }
                output.getType().toFile(cacheFile, argument);
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
        int cachedHashCode = 0;
        if (infoFile.exists()) {
            try (var sr = new BufferedInputStream(new FileInputStream(infoFile))) {
                cachedHashCode = ByteBuffer.wrap(sr.readAllBytes()).getInt();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (cachedHashCode != getHashFor(node, currentInputs)) {
            return Optional.empty();
        }

        var nodesArguments = new NodeArguments();
        for (var outputConnector : node.getOutputs().values()) {
            var cacheFile = getOutputConnectorFile(nodeCacheDirectory, outputConnector);
            if (cacheFile.exists()) {
                var obj = outputConnector.getType().fromFile(cacheFile);
                obj.ifPresent(o -> nodesArguments.putArgument(outputConnector.getName(), o));
            }
        }

        return Optional.of(nodesArguments);
    }

    public synchronized void clear() {
        Utils.deleteCompleteDirectory(cacheDirectory);
    }

    public static void clearAll() {
        Utils.deleteCompleteDirectory(cacheRootDirectory);
    }
}
