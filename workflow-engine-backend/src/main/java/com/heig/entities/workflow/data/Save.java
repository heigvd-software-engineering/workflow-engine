package com.heig.entities.workflow.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.heig.entities.workflow.execution.NodeState;
import com.heig.entities.workflow.execution.WorkflowExecutionListener;
import com.heig.entities.workflow.execution.WorkflowExecutor;
import com.heig.entities.workflow.execution.WorkflowManager;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.*;

public class Save {
    private final File saveDirectory;
    private final WorkflowExecutor workflowExecutor;

    private static final String saveFileName = "workflowExecutor.obj";

    Save(@Nonnull WorkflowExecutionListener listener, @Nonnull File rootDirectory) {
        this.saveDirectory = rootDirectory;

        this.workflowExecutor = load(listener).orElseThrow(() -> new RuntimeException("The save for this workflow was not found"));
    }

    Save(@Nonnull WorkflowExecutor we, @Nonnull File rootDirectory) {
        Objects.requireNonNull(we);
        Objects.requireNonNull(rootDirectory);

        saveDirectory = new File(rootDirectory, "save");
        if (!saveDirectory.mkdirs()) {
            throw new RuntimeException("Could not create workflow save directory");
        }
        workflowExecutor = we;
    }

    public void save() {
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

    public Optional<WorkflowExecutor> load(@Nonnull WorkflowExecutionListener listener) {
        Objects.requireNonNull(listener);

        var file = new File(saveDirectory, saveFileName);
        if (!file.exists()) {
            return Optional.empty();
        }

        var deserializer = new WorkflowExecutor.Deserializer(listener);

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
