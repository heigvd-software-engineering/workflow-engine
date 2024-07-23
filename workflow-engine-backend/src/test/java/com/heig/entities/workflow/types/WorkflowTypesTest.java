package com.heig.entities.workflow.types;

import com.heig.entities.workflow.file.FileWrapper;
import com.heig.testHelpers.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
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

        //Test for WFlow
        assert WorkflowTypes.fromObject(WFlow.of()) == WFlow.of();

        //Should not be able to create a list of WFlow
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WorkflowTypes.fromObject(List.of(WFlow.of()));
        });

        //Should not be able to create a map with WFlow as value
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WorkflowTypes.fromObject(Map.of(1, WFlow.of()));
        });

        //Should not be able to create a map with WFlow as key
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WorkflowTypes.fromObject(Map.of(WFlow.of(), 1));
        });

        //Should not be able to create a list of list of WFlow
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WorkflowTypes.fromObject(List.of(List.of(WFlow.of())));
        });
    }

    private boolean testType(WType type) {
        return WorkflowTypes.typeFromString(WorkflowTypes.typeToString(type)) == type;
    }

    @Test
    public void typeStringConverter() {
        assert testType(WPrimitive.String);
        assert testType(WPrimitive.Integer);

        assert testType(WFlow.of());
        assert testType(WObject.of());

        assert testType(WCollection.of(WPrimitive.String));
        assert testType(WCollection.of(WObject.of()));
        assert testType(WCollection.of(WCollection.of(WPrimitive.Double)));

        assert testType(WMap.of(WObject.of(), WCollection.of(WPrimitive.Double)));
        assert testType(WMap.of(WPrimitive.Byte, WMap.of(WObject.of(), WPrimitive.Boolean)));
        assert testType(WCollection.of(WMap.of(WMap.of(WPrimitive.Character, WPrimitive.Long), WMap.of(WObject.of(), WPrimitive.Integer))));
    }

    @Test
    public void hashCodeTest() {
        assert TestUtils.hashCodeTest(WFlow::of);

        assert TestUtils.hashCodeTest(() -> List.of(Map.of(Map.of('a', (long) 1), Map.of(22, 34))));
        assert TestUtils.hashCodeTest(() -> List.of(Map.of(Map.of('a', (short) 1), Map.of((double)22, 34))));
        assert TestUtils.hashCodeTest(() -> List.of(1, 2, "3", List.of(1, 2, 3, 4)));
        assert TestUtils.hashCodeTest(() -> List.of(Map.of(Map.of(), Map.of(22, 34))));

        var fwNotExists = new FileWrapper("notExists.file");
        if (fwNotExists.exists()) {
            assert fwNotExists.delete();
        }

        var fwEmpty = new FileWrapper("empty.file");
        assert fwEmpty.createOrReplace();

        var fwContent1 = new FileWrapper("content1.file");
        assert fwContent1.createOrReplace();
        try (var writer = fwContent1.writer()) {
            writer.write("This is a test");
        }

        var fwContent2 = new FileWrapper("content2.file");
        assert fwContent2.createOrReplace();
        try (var writer = fwContent2.writer()) {
            writer.write("This is another test");
        }

        assert TestUtils.hashCodeTest(() -> fwNotExists);
        assert TestUtils.hashCodeTest(() -> fwEmpty);
        assert TestUtils.hashCodeTest(() -> fwContent1);
        assert TestUtils.hashCodeTest(() -> fwContent2);

        var lstFiles = List.of(fwNotExists, fwEmpty, fwContent1, fwContent2);

        for (int i = 0; i < lstFiles.size(); i++) {
            var hcI = WFile.of().getHashCode(lstFiles.get(i));
            for (int j = 0; j < lstFiles.size(); j++) {
                if (i == j) {
                    continue;
                }
                var hcJ = WFile.of().getHashCode(lstFiles.get(j));
                assert hcI != hcJ;
            }
        }

        assert lstFiles.stream().allMatch(fw -> {
            if (fw.exists()) {
                return fw.delete();
            }
            return true;
        });
    }
}
