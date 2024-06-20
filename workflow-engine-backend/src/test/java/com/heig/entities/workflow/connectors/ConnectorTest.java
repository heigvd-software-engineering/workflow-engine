package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.nodes.CodeNode;
import com.heig.entities.workflow.types.WObject;
import com.heig.entities.workflow.types.WPrimitive;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ConnectorTest {
    private CodeNode createCodeNode() {
        var w = new Workflow();
        return w.createCodeNode();
    }

    @Test
    public void create() {
        var n = createCodeNode();

        var connector = n.createInputConnector("connector");
        Assertions.assertThrows(NullPointerException.class, () -> {
            connector.setType(null);
        });

        //Default
        assert connector.getType() == WObject.of();

        //Setting the type
        connector.setType(WPrimitive.Double);
        assert connector.getType() == WPrimitive.Double;
    }

    @Test
    public void input() {
        var n = createCodeNode();

        var connector = n.createInputConnector("input");
        var outputConnector = n.createOutputConnector("output");

        //Empty
        assert connector.getConnectedTo().isEmpty();

        //Setting node
        connector.setConnectedTo(outputConnector);
        assert connector.getConnectedTo().isPresent();
        assert connector.getConnectedTo().get().equals(outputConnector);

        //Setting no node
        connector.setConnectedTo(null);
        assert connector.getConnectedTo().isEmpty();
    }

    @Test
    public void output() {
        var n = createCodeNode();

        var connector = n.createOutputConnector("output");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            n.createOutputConnector("output");
        });

        var inputConnector = n.createInputConnector("input");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            n.createInputConnector("input");
        });

        //We should not be able to add elements directly
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            connector.getConnectedTo().add(inputConnector);
        });

        //Empty
        assert connector.getConnectedTo().isEmpty();

        //We should not be able to add null nodes
        Assertions.assertThrows(NullPointerException.class, () -> {
            connector.addConnectedTo(null);
        });

        //Adding node
        assert connector.addConnectedTo(inputConnector);
        assert connector.getConnectedTo().size() == 1;

        //Adding already existent node
        assert !connector.addConnectedTo(inputConnector);

        //Removing node
        assert connector.removeConnectedTo(inputConnector);
        assert connector.getConnectedTo().isEmpty();

        //Removing non-existent node
        assert !connector.removeConnectedTo(inputConnector);
    }
}
