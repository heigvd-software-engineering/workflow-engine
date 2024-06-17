package com.heig.entities;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ConnectorTest {
    @Test
    public void create() {
        var connector = new InputConnector();
        Assertions.assertThrows(NullPointerException.class, () -> {
            connector.setType(null);
        });

        //Default
        assert connector.getType() == WorkflowTypes.Object;

        //Setting the type
        connector.setType(WorkflowTypes.Double);
        assert connector.getType() == WorkflowTypes.Double;
    }

    @Test
    public void input() {
        var connector = new InputConnector();
        var outputConnector = new OutputConnector();

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
        var connector = new OutputConnector();
        var inputConnector = new InputConnector();

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
