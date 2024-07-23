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

/**
 * Class storing an instance of {@link Save} and {@link Cache} for each workflow UUID
 * Get instance with {@link Data#get(UUID)}, {@link Data#getOrCreate(WorkflowExecutor)} or {@link Data#loadFromSave(UUID, WorkflowExecutionListener)}
 */
public class Data {
    /**
     * The root directory loaded for the config of quarkus
     */
    public static final File dataRootDirectory = new File(ConfigProvider.getConfig().getValue("data_directory", String.class));

    /**
     * The instances of Data for each workflow UUID
     */
    private static final ConcurrentMap<UUID, Data> instances = new ConcurrentHashMap<>();

    /**
     * The {@link Cache}
     */
    private final Cache cache;

    /**
     * The {@link Save}
     */
    private final Save save;

    /**
     * The data directory (directory named with the workflow UUID under the root directory)
     */
    private final File dataDirectory;

    private Data(@Nonnull UUID workflowUUID, @Nonnull WorkflowExecutionListener listener) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(listener);

        dataDirectory = new File(dataRootDirectory, workflowUUID.toString());
        if (!dataDirectory.exists()) {
            throw new RuntimeException("Folder containing the save data doesn't exist");
        }

        save = new Save(this, listener, dataDirectory);
        cache = new Cache(save.getWorkflowExecutor().getWorkflow(), dataDirectory);
    }

    private Data(@Nonnull WorkflowExecutor we) {
        Objects.requireNonNull(we);

        dataDirectory = new File(dataRootDirectory, we.getWorkflow().getUUID().toString());
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdirs()) {
                throw new RuntimeException("Could not create workflow data directory");
            }
        }

        save = new Save(we, dataDirectory);
        cache = new Cache(we.getWorkflow(), dataDirectory);
    }

    public Cache getCache() {
        return cache;
    }

    public Save getSave() {
        return save;
    }

    /**
     * Deletes the data directory for a single workflow UUID
     */
    public void delete() {
        Utils.deleteCompleteDirectory(dataDirectory);
    }

    /**
     * Deletes the data root directory
     */
    public static void clearAll() {
        Utils.deleteCompleteDirectory(dataRootDirectory);
    }

    /**
     * Gets the {@link Data} instance from the instances pool or creates a new instance and puts it in the pool
     * @param we The {@link WorkflowExecutor}
     * @return The {@link Data} for UUID of the workflow linked to the {@link WorkflowExecutor}
     */
    public static Data getOrCreate(@Nonnull WorkflowExecutor we) {
        return instances.computeIfAbsent(we.getWorkflow().getUUID(), (uuid) -> new Data(we));
    }

    /**
     * Loads the state of the workflow from a save file
     * @param workflowUUID The workflow UUID to search the save file for
     * @param listener The listener to use when creating the {@link WorkflowExecutor}
     * @return The {@link Data} for the workflow UUID
     */
    public static Data loadFromSave(@Nonnull UUID workflowUUID, @Nonnull WorkflowExecutionListener listener) {
        var data = new Data(workflowUUID, listener);
        instances.put(workflowUUID, data);
        return data;
    }

    /**
     * Gets the {@link Data} instance from the instances pool
     * @param workflowUUID The workflow UUID
     * @return The {@link Data} for UUID of the workflow linked to the {@link WorkflowExecutor} or {@link Optional#empty()} if the {@link Data} was not found
     */
    public static Optional<Data> get(@Nonnull UUID workflowUUID) {
        return Optional.ofNullable(instances.get(workflowUUID));
    }

    /**
     * Used to write an object to an output file with {@link ObjectOutputStream}
     * @param output The output file
     * @param value The value to write to the file
     */
    public static void toFile(@Nonnull File output, @Nonnull Object value) {
        Objects.requireNonNull(output);
        Objects.requireNonNull(value);
        try (var oos = new ObjectOutputStream(new FileOutputStream(output))) {
            oos.writeObject(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Used to read an object from an input file with {@link ObjectInputStream}
     * @param input The input file
     * @return The object read or {@link Optional#empty()} if an error was thrown
     */
    public static Optional<Object> fromFile(@Nonnull File input) {
        Objects.requireNonNull(input);
        try (var ois = new ObjectInputStream(new FileInputStream(input))) {
            return Optional.ofNullable(ois.readObject());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
