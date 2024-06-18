package com.heig.entities;

import com.heig.entities.workflowTypes.WObject;
import com.heig.entities.workflowTypes.WPrimitive;
import com.heig.entities.workflowTypes.WPrimitiveTypes;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ConnectorTest {
    private Node createNode() {
        var w = new Workflow();
        return w.createNode();
    }

    @Test
    public void create() {
        var n = createNode();

        var connector = n.createInputConnector();
        Assertions.assertThrows(NullPointerException.class, () -> {
            connector.setType(null);
        });

        //Default
        assert connector.getType() == WObject.of();

        //Setting the type
        connector.setType(WPrimitive.of(WPrimitiveTypes.Double));
        assert connector.getType() == WPrimitive.of(WPrimitiveTypes.Double);
    }

    @Test
    public void input() {
        var n = createNode();

        var connector = n.createInputConnector();
        var outputConnector = n.createOutputConnector();

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
        var n = createNode();

        var connector = n.createOutputConnector();
        var inputConnector = n.createInputConnector();

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
