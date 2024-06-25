package com.heig.entities.workflow.cache;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.nodes.Node;
import com.heig.helpers.Utils;
import io.smallrye.common.annotation.CheckReturnValue;
import jakarta.annotation.Nonnull;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

    @CheckReturnValue
    public synchronized void set(@Nonnull Node node, @Nonnull NodeArguments arguments) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(arguments);
        var nodeCacheDirectory = getNodeCacheDirectory(node);
        if (nodeCacheDirectory.exists()) {
            var files = nodeCacheDirectory.listFiles();
            if (files == null) {
                throw new RuntimeException("Error while listing files in cache directory");
            }
            for (var file : files) {
                if (!file.delete()) {
                    throw new RuntimeException("Could not delete file " + file);
                }
            }
            if (!nodeCacheDirectory.delete()) {
                throw new RuntimeException("Could not delete cache directory");
            }
        }
        if (!nodeCacheDirectory.mkdir()) {
            throw new RuntimeException("Could not create cache directory");
        }

        for (var argumentName : arguments.getArguments().keySet()) {
            var outputConnectorOpt = node.getOutputs().values().stream()
                    .filter(o -> o.getName().equals(argumentName))
                    .findFirst();
            if (outputConnectorOpt.isEmpty()) {
                continue;
            }
            var outputConnector = outputConnectorOpt.get();

            var argument = arguments.getArguments().get(argumentName);

            var cacheFile = getOutputConnectorFile(nodeCacheDirectory, outputConnector);
            try {
                if (!cacheFile.createNewFile()) {
                    throw new RuntimeException("Could not create cache file");
                }
                outputConnector.getType().toFile(cacheFile, argument);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized Optional<NodeArguments> get(@Nonnull Node node) {
        Objects.requireNonNull(node);
        var nodeCacheDirectory = getNodeCacheDirectory(node);
        if (!nodeCacheDirectory.exists() || node.getOutputs().isEmpty()) {
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
