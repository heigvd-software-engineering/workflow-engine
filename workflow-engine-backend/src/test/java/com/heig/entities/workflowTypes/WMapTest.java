package com.heig.entities.workflowTypes;

import com.heig.testHelpers.TestUtils;
import org.junit.jupiter.api.Test;

public class WMapTest {
    @Test
    public void onlyOneInstance() {
        assert WMap.of(WPrimitive.of(WPrimitiveTypes.Integer), WObject.of()) == WMap.of(WPrimitive.of(WPrimitiveTypes.Integer), WObject.of());
    }

    @Test
    public void compatibility() {
        var integer = WPrimitive.of(WPrimitiveTypes.Integer);
        var string = WPrimitive.of(WPrimitiveTypes.String);
        var obj = WObject.of();
        var intStringMap = WMap.of(integer, string);
        var stringIntMap = WMap.of(string, integer);
        var intObjectMap = WMap.of(integer, obj);
        var objectStringMap = WMap.of(obj, string);

        //Map<Integer, String> is compatible with Map<Integer, String>
        assert intStringMap.isCompatibleWith(intStringMap);

        //Map<Integer, String> is compatible with Object
        assert TestUtils.checkDoubleCompatibility(intStringMap, obj);

        //Map<Integer, String> is not compatible with Map<String, Integer>
        assert !TestUtils.checkDoubleCompatibility(intStringMap, stringIntMap);

        //Map<Integer, String> is compatible with Map<Integer, Object>
        assert TestUtils.checkDoubleCompatibility(intStringMap, intObjectMap);

        //Map<Integer, String> is compatible with Map<Object, String>
        assert TestUtils.checkDoubleCompatibility(intStringMap, objectStringMap);

        //Map<String, Integer> is not compatible with String
        assert !TestUtils.checkDoubleCompatibility(stringIntMap, string);
    }
}
