package com.heig.entities.workflow.data;

import com.heig.entities.workflow.execution.WorkflowExecutionListener;
import com.heig.entities.workflow.execution.WorkflowExecutor;
import com.heig.helpers.Utils;
import jakarta.annotation.Nonnull;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.*;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Data {
    public static final File dataRootDirectory = new File(ConfigProvider.getConfig().getValue("data_directory", String.class));
    private static final ConcurrentMap<UUID, Data> instances = new ConcurrentHashMap<>();

    private final Cache cache;
    private final Save save;

    private Data(@Nonnull UUID workflowUUID, @Nonnull WorkflowExecutionListener listener) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(listener);

        var dataDirectory = new File(dataRootDirectory, workflowUUID.toString());
        if (!dataDirectory.exists()) {
            throw new RuntimeException("Folder containing the save data doesn't exist");
        }

        save = new Save(listener, dataDirectory);
        cache = new Cache(save.getWorkflowExecutor().getWorkflow(), dataDirectory);
    }

    private Data(@Nonnull WorkflowExecutor we) {
        Objects.requireNonNull(we);

        var dataDirectory = new File(dataRootDirectory, we.getWorkflow().getUUID().toString());
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdirs()) {
                throw new RuntimeException("Could not create workflow data directory");
            }
        }

        cache = new Cache(we.getWorkflow(), dataDirectory);
        save = new Save(we, dataDirectory);
    }

    public Cache getCache() {
        return cache;
    }

    public Save getSave() {
        return save;
    }

    public static void clearAll() {
        Utils.deleteCompleteDirectory(dataRootDirectory);
    }

    public static Data getOrCreate(@Nonnull WorkflowExecutor we) {
        return instances.computeIfAbsent(we.getWorkflow().getUUID(), (uuid) -> new Data(we));
    }

    public static Data loadFromSave(@Nonnull UUID workflowUUID, @Nonnull WorkflowExecutionListener listener) {
        return instances.put(workflowUUID, new Data(workflowUUID, listener));
    }

    public static Optional<Data> get(@Nonnull UUID workflowUUID) {
        return Optional.ofNullable(instances.get(workflowUUID));
    }

    public static void toFile(@Nonnull File output, @Nonnull Object value) {
        Objects.requireNonNull(output);
        Objects.requireNonNull(value);
        try (var oos = new ObjectOutputStream(new FileOutputStream(output))) {
            oos.writeObject(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<Object> fromFile(@Nonnull File input) {
        Objects.requireNonNull(input);
        try (var ois = new ObjectInputStream(new FileInputStream(input))) {
            return Optional.ofNullable(ois.readObject());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
