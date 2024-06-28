package com.heig.entities.workflow;

import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

public interface NodeModifiedListener {
    void nodeModified(@Nonnull Node node);
}
