package com.heig.testHelpers;

import com.heig.entities.Node;
import com.heig.entities.OutputConnector;
import com.heig.entities.Workflow;
import com.heig.entities.workflowTypes.WType;
import groovy.lang.Tuple;
import groovy.lang.Tuple2;

public class TestUtils {
    public static Tuple2<OutputConnector, Node> createPrimitiveNode(Workflow w, WType type) {
        var node = w.createNode();

        var output = node.createOutputConnector();
        output.setType(type);

        return Tuple.tuple(output, node);
    }

    public static boolean checkDoubleCompatibility(WType t1, WType t2) {
        return t1.isCompatibleWith(t2) && t2.isCompatibleWith(t1);
    }
}
