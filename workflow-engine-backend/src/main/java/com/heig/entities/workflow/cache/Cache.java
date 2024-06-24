package com.heig.entities.workflow.cache;

import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;


public class Cache {
    private static final File cacheDirectory = new File("D:\\TB\\cache");
    public synchronized void set(@Nonnull Node node, @Nonnull NodeArguments arguments) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(arguments);
        var nodeCacheDirectory = new File(cacheDirectory, String.valueOf(node.getId()));
        if (nodeCacheDirectory.exists()) {
            if (!nodeCacheDirectory.delete()) {
                throw new RuntimeException("Could not delete cache directory");
            }
        }
        if (!nodeCacheDirectory.mkdir()) {
            throw new RuntimeException("Could not create cache directory");
        }

        for (var argumentName : arguments.getArguments().keySet()) {
            var outputConnector = node.getOutputs().values().stream()
                    .filter(o -> o.getName().equals(argumentName))
                    .findFirst()
                    .get();
            var argument = arguments.getArguments().get(argumentName);

            var cache = new File(nodeCacheDirectory, outputConnector.getId() + ".cache");
            if (cache.exists()) {
                if (!cache.delete()) {
                    throw new RuntimeException("Could not delete cache file");
                }
            }
            try {
                if (!cache.createNewFile()) {
                    throw new RuntimeException("Could not create cache file");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            outputConnector.getType().toFile(cache, argument);
        }
    }

    public synchronized Optional<Object> get(@Nonnull OutputConnector connector) {
        Objects.requireNonNull(connector);
        var nodeCacheDirectory = new File(cacheDirectory, String.valueOf(connector.getParent().getId()));
        var cache = new File(nodeCacheDirectory, connector.getId() + ".cache");
        if (cache.exists()) {
            return Optional.ofNullable(connector.getType().fromFile(cache));
        }
        return Optional.empty();
    }
}
