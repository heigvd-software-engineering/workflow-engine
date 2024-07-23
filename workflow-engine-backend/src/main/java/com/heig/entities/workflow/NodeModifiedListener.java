package com.heig.entities.workflow;

import com.heig.entities.workflow.nodes.Node;
import jakarta.annotation.Nonnull;

/**
 * Interface used to notify that something in the {@link Node} has changed
 */
public interface NodeModifiedListener {
    /**
     * Called when the {@link Node} has changed
     * @param node The {@link Node}
     */
    void nodeModified(@Nonnull Node node);
}
