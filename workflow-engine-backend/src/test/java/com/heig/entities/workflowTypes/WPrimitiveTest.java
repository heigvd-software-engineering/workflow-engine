package com.heig.entities.workflowTypes;

import com.heig.testHelpers.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WPrimitiveTest {
    @Test
    public void onlyOneInstance() {
        assert WPrimitive.of(WPrimitiveTypes.Integer) == WPrimitive.of(WPrimitiveTypes.Integer);
    }

    @Test
    public void compatibility() {
        var object = WObject.of();
        var string = WPrimitive.of(WPrimitiveTypes.String);
        var integer = WPrimitive.of(WPrimitiveTypes.Integer);
        var doubleT = WPrimitive.of(WPrimitiveTypes.Double);

        //String is compatible with string
        assert string.isCompatibleWith(string);

        //String is not compatible with Integer
        assert !TestUtils.checkDoubleCompatibility(string, integer);

        //Double is not compatible with Integer
        assert !TestUtils.checkDoubleCompatibility(doubleT, integer);

        //Object is compatible with Integer
        assert TestUtils.checkDoubleCompatibility(object, integer);
    }
}
