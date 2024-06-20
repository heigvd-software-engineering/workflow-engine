package com.heig.entities.workflow.types;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

@QuarkusTest
public class WorkflowTypesTest {
    @Test
    public void test() {
        //Test the types of the primitives
        assert WorkflowTypes.fromObject(1) == WPrimitive.Integer;
        assert WorkflowTypes.fromObject("String") == WPrimitive.String;
        assert WorkflowTypes.fromObject(true) == WPrimitive.Boolean;
        assert WorkflowTypes.fromObject((byte) 0) == WPrimitive.Byte;
        assert WorkflowTypes.fromObject((short) 0) == WPrimitive.Short;
        assert WorkflowTypes.fromObject((long) 0) == WPrimitive.Long;
        assert WorkflowTypes.fromObject((float) 0) == WPrimitive.Float;
        assert WorkflowTypes.fromObject(0.0) == WPrimitive.Double;
        assert WorkflowTypes.fromObject('c') == WPrimitive.Character;

        //Test the type of the lists
        assert WorkflowTypes.fromObject(List.of(1)) == WCollection.of(WPrimitive.Integer);
        assert WorkflowTypes.fromObject(Set.of(1)) == WCollection.of(WPrimitive.Integer);
        assert WorkflowTypes.fromObject(List.of(1, List.of(1))) == WCollection.of(WObject.of());
        assert WorkflowTypes.fromObject(List.of(List.of("test"))) == WCollection.of(WCollection.of(WPrimitive.String));
        assert WorkflowTypes.fromObject(List.of(List.of(1), List.of(2))) == WCollection.of(WCollection.of(WPrimitive.Integer));
        assert WorkflowTypes.fromObject(List.of(List.of(1), List.of("test"))) == WCollection.of(WCollection.of(WObject.of()));

        //Test the type of the maps
        assert WorkflowTypes.fromObject(Map.of(1, "test1", 2, "test2")) == WMap.of(WPrimitive.Integer, WPrimitive.String);
        assert WorkflowTypes.fromObject(Map.of(1, "test1", 2, 3)) == WMap.of(WPrimitive.Integer, WObject.of());
        assert WorkflowTypes.fromObject(Map.of("1", "test1", 2, "test2")) == WMap.of(WObject.of(), WPrimitive.String);

        //Test the type for more complex types
        assert WorkflowTypes.fromObject(List.of(List.of(Map.of(1, 2)), List.of(Map.of("a", 2)))) == WCollection.of(WCollection.of(WMap.of(WObject.of(), WPrimitive.Integer)));
        assert WorkflowTypes.fromObject(List.of(List.of(Map.of(List.of(1), 2)), List.of(Map.of(List.of("a"), 2)))) == WCollection.of(WCollection.of(WMap.of(WCollection.of(WObject.of()), WPrimitive.Integer)));
    }
}
