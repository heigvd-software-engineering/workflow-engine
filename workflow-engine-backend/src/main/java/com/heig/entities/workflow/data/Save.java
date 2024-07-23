package com.heig.entities.workflow.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.heig.entities.workflow.execution.WorkflowExecutionListener;
import com.heig.entities.workflow.execution.WorkflowExecutor;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.*;

/**
 * Used the save the workflow
 */
public class Save {
    /**
     * The save directory
     */
    private final File saveDirectory;

    /**
     * The workflow executor
     */
    private final WorkflowExecutor workflowExecutor;

    /**
     * The name of the save file
     */
    private static final String saveFileName = "workflowExecutor.json";

    /**
     * Returns the save directory from the root directory
     * @param rootDirectory The root directory
     * @return The save directory
     */
    private File getSaveDirectory(File rootDirectory) {
        var saveDirectory = new File(rootDirectory, "save");
        if (!saveDirectory.exists() && !saveDirectory.mkdirs()) {
            throw new RuntimeException("Could not create workflow save directory");
        }
        return saveDirectory;
    }

    Save(@Nonnull Data data, @Nonnull WorkflowExecutionListener listener, @Nonnull File rootDirectory) {
        this.saveDirectory = getSaveDirectory(rootDirectory);
        this.workflowExecutor = load(data, listener).orElseThrow(() -> new RuntimeException("The save for this workflow was not found"));
    }

    Save(@Nonnull WorkflowExecutor we, @Nonnull File rootDirectory) {
        Objects.requireNonNull(we);
        Objects.requireNonNull(rootDirectory);

        saveDirectory = getSaveDirectory(rootDirectory);
        workflowExecutor = we;

        //If the Save object was created using a WorkflowExecutor directly, we save the object
        save();
    }

    /**
     * Saves the current {@link WorkflowExecutor}
     */
    public synchronized void save() {
        var serializer = new WorkflowExecutor.Serializer();
        var json = new Gson().toJson(serializer.serialize(workflowExecutor));
        var file = new File(saveDirectory, saveFileName);
        if (file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException("Could not delete old workflow save file");
            }
        }
        try {
            if (!file.createNewFile()) {
                throw new RuntimeException("Could not create workflow save file");
            }
            try (var writer = new FileWriter(file)) {
                writer.write(json);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a {@link WorkflowExecutor} loaded from the save file
     * @param data The {@link Data} object representing the workflow to load
     * @param listener The listener to attach when creating the {@link WorkflowExecutor}
     * @return The {@link WorkflowExecutor} loaded from the save file or {@link Optional#empty()} if the loading failed
     */
    public synchronized Optional<WorkflowExecutor> load(@Nonnull Data data, @Nonnull WorkflowExecutionListener listener) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(listener);

        var file = new File(saveDirectory, saveFileName);
        if (!file.exists()) {
            return Optional.empty();
        }

        var deserializer = new WorkflowExecutor.Deserializer(data, listener);

        try (var reader = new BufferedReader(new FileReader(file))) {
            var json = reader.readLine();
            var we = deserializer.deserialize(new Gson().fromJson(json, JsonElement.class));
            return Optional.ofNullable(we);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public WorkflowExecutor getWorkflowExecutor() {
        return workflowExecutor;
    }
}
