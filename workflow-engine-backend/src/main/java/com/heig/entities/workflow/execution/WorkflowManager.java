package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.cache.Cache;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WorkflowManager {
    private WorkflowManager() {}

    private static final ConcurrentMap<UUID, WorkflowExecutor> workflowExecutors = new ConcurrentHashMap<>();

    public static WorkflowExecutor createWorkflowExecutor(@Nonnull Workflow workflow, @Nonnull WorkflowExecutionListener workflowExecutionListener) {
        Objects.requireNonNull(workflow);
        Objects.requireNonNull(workflowExecutionListener);

        var we = new WorkflowExecutor(workflow, workflowExecutionListener);
        workflowExecutors.put(workflow.getUuid(), we);
        return we;
    }

    public static Optional<WorkflowExecutor> getWorkflowExecutor(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid);
        return Optional.ofNullable(workflowExecutors.get(uuid));
    }

    public static boolean removeWorkflowExecutor(@Nonnull WorkflowExecutor w) {
        Objects.requireNonNull(w);
        w.clearCache();
        return workflowExecutors.remove(w.getWorkflow().getUuid()) != null;
    }
}
