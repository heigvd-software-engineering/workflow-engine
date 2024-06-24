package com.heig.entities.workflow.connectors;

import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.errors.UnmodifiableConnector;
import com.heig.entities.workflow.nodes.CodeNode;
import com.heig.entities.workflow.nodes.PrimitiveNode;
import com.heig.entities.workflow.types.WObject;
import com.heig.entities.workflow.types.WPrimitive;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

@QuarkusTest
public class ConnectorTest {
    private CodeNode createCodeNode(Workflow w) {
        return w.getNodeBuilder().buildCodeNode();
    }

    @Test
    public void create() {
        var w = new Workflow();
        var n = createCodeNode(w);

        var connector = n.getConnectorBuilder().buildInputConnector("connector", WObject.of());
        Assertions.assertThrows(NullPointerException.class, () -> {
            connector.setType(null);
        });

        //Default
        assert connector.getType() == WObject.of();

        //Setting the type
        assert connector.setType(WPrimitive.Double).isEmpty();
        assert connector.getType() == WPrimitive.Double;

        //Setting the name
        assert connector.setName("conn").isEmpty();
        //If I want to set the same name again I should not be getting an error
        assert connector.setName("conn").isEmpty();
        assert Objects.equals(connector.getName(), "conn");

        //Creating a primitive node (cannot change the output connector name or type after instantiation)
        var p = w.getNodeBuilder().buildPrimitiveNode(WPrimitive.Integer);
        assert Objects.equals(p.getOutputConnector().getName(), PrimitiveNode.OUTPUT_NAME);
        assert p.getOutputConnector().getType() == WPrimitive.Integer;

        var optError = p.getOutputConnector().setType(WPrimitive.Double);
        assert optError.isPresent() && optError.get().getClass() == UnmodifiableConnector.class;
        optError = p.getOutputConnector().setName("test");
        assert optError.isPresent() && optError.get().getClass() == UnmodifiableConnector.class;
    }

    @Test
    public void input() {
        var w = new Workflow();
        var n = createCodeNode(w);

        var connector = n.getConnectorBuilder().buildInputConnector("input", WObject.of());
        var outputConnector = n.getConnectorBuilder().buildOutputConnector("output", WObject.of());

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
        var w = new Workflow();
        var n = createCodeNode(w);

        var connector = n.getConnectorBuilder().buildOutputConnector("conn", WObject.of());
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            n.getConnectorBuilder().buildOutputConnector("conn", WObject.of());
        });

        var inputConnector = n.getConnectorBuilder().buildInputConnector("conn", WObject.of());
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            n.getConnectorBuilder().buildInputConnector("conn", WObject.of());
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
