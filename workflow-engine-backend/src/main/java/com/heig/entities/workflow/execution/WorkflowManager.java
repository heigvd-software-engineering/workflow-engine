package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.Workflow;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
