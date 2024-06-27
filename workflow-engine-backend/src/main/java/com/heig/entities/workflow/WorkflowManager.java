package com.heig.entities.workflow;

import com.heig.entities.workflow.cache.Cache;
import com.heig.entities.workflow.execution.WorkflowExecutor;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WorkflowManager {
    private WorkflowManager() {}

    private static final ConcurrentMap<UUID, Workflow> workflows = new ConcurrentHashMap<>();

    public static Workflow createWorkflow(@Nonnull String name) {
        var uuid = UUID.randomUUID();
        var workflow = new Workflow(uuid, name);
        workflows.put(uuid, workflow);
        return workflow;
    }

    public static Optional<Workflow> getWorkflow(UUID uuid) {
        return Optional.ofNullable(workflows.get(uuid));
    }

    public static boolean removeWorkflow(@Nonnull Workflow w) {
        Objects.requireNonNull(w);
        Cache.get(w).clear();
        return workflows.remove(w.getUuid()) != null;
    }
}
