package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.data.Data;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class WorkflowManager {
    private WorkflowManager() {}

    private static final ConcurrentMap<UUID, WorkflowExecutor> workflowExecutors = new ConcurrentHashMap<>();

    public static WorkflowExecutor createWorkflowExecutor(@Nonnull Workflow workflow, @Nonnull WorkflowExecutionListener workflowExecutionListener) {
        Objects.requireNonNull(workflow);
        Objects.requireNonNull(workflowExecutionListener);

        var we = new WorkflowExecutor(workflow, workflowExecutionListener);
        workflowExecutors.put(workflow.getUUID(), we);
        return we;
    }

    public static void loadExistingWorkflows(@Nonnull Function<UUID, Tuple2<WorkflowExecutionListener, Consumer<WorkflowExecutor>>> supplier) {
        Objects.requireNonNull(supplier);

        if (Data.dataRootDirectory.exists()) {
            var workflowsUUIDs = Data.dataRootDirectory.list((dir, name) -> new File(dir, name).isDirectory());
            if (workflowsUUIDs != null) {
                for (var workflowUUID : workflowsUUIDs) {
                    var uuid = UUID.fromString(workflowUUID);
                    var tuple = supplier.apply(uuid);
                    var data = Data.loadFromSave(uuid, tuple.getItem1());
                    var we = data.getSave().getWorkflowExecutor();
                    workflowExecutors.put(we.getWorkflow().getUUID(), we);
                    tuple.getItem2().accept(we);
                }
            }
        }
    }

    public static Optional<WorkflowExecutor> getWorkflowExecutor(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid);
        return Optional.ofNullable(workflowExecutors.get(uuid));
    }

    public static boolean removeWorkflowExecutor(@Nonnull WorkflowExecutor we) {
        Objects.requireNonNull(we);
        we.delete();
        return workflowExecutors.remove(we.getWorkflow().getUUID()) != null;
    }

    public static List<WorkflowExecutor> getWorkflowExecutors() {
        return workflowExecutors.values().stream().toList();
    }
}
