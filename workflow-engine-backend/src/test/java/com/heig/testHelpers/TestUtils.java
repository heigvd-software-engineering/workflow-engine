package com.heig.testHelpers;

import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.nodes.PrimitiveNode;
import com.heig.entities.workflow.types.WType;
import groovy.lang.Tuple;
import groovy.lang.Tuple2;

public class TestUtils {
    public static Tuple2<OutputConnector, PrimitiveNode> createPrimitiveNode(Workflow w, WType type) {
        var node = w.getNodeBuilder().buildPrimitiveNode(type);

        return Tuple.tuple(node.getOutputConnector(), node);
    }
}
