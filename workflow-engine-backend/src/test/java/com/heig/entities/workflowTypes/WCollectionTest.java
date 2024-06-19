package com.heig.entities.workflowTypes;

import com.heig.testHelpers.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WCollectionTest {
    @Test
    public void onlyOneInstance() {
        assert WCollection.of(WObject.of()) == WCollection.of(WObject.of());
    }

    @Test
    public void compatibility() {
        var integer = WPrimitive.of(WPrimitiveTypes.Integer);
        var intList = WCollection.of(integer);
        var doubleList = WCollection.of(WPrimitive.of(WPrimitiveTypes.Double));
        var obj = WObject.of();
        var objList = WCollection.of(WObject.of());

        //Collection<Integer> is compatible with Collection<Integer>
        assert intList.isCompatibleWith(intList);

        //Collection<Integer> is compatible with Object
        assert TestUtils.checkDoubleCompatibility(intList, obj);

        //Collection<Integer> is compatible with Collection<Object>
        assert TestUtils.checkDoubleCompatibility(intList, objList);

        //Collection<Integer> is not compatible with Collection<Double>
        assert !TestUtils.checkDoubleCompatibility(intList, doubleList);

        //Collection<Object> is not compatible with Integer
        assert !TestUtils.checkDoubleCompatibility(objList, integer);
    }
}
