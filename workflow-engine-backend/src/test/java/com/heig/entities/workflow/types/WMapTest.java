package com.heig.entities.workflow.types;

import org.junit.jupiter.api.Test;

public class WMapTest {
    @Test
    public void onlyOneInstance() {
        assert WMap.of(WPrimitive.Integer, WObject.of()) == WMap.of(WPrimitive.Integer, WObject.of());
    }

    @Test
    public void compatibility() {
        var integer = WPrimitive.Integer;
        var string = WPrimitive.String;
        var obj = WObject.of();
        var intStringMap = WMap.of(integer, string);
        var stringIntMap = WMap.of(string, integer);
        var intObjectMap = WMap.of(integer, obj);
        var objectStringMap = WMap.of(obj, string);

        //Map<Integer, String> can be converted to Map<Integer, String>
        assert intStringMap.canBeConvertedFrom(intStringMap);

        //Map<Integer, String> can be converted to Object
        assert obj.canBeConvertedFrom(intStringMap);

        //Map<Integer, String> cannot be converted to Map<String, Integer>
        assert !intStringMap.canBeConvertedFrom(stringIntMap);
        assert !stringIntMap.canBeConvertedFrom(intStringMap);

        //Map<Integer, String> can be converted to Map<Integer, Object>
        assert intObjectMap.canBeConvertedFrom(intStringMap);
        //but the inverse is not true
        assert !intStringMap.canBeConvertedFrom(intObjectMap);

        //Map<Integer, String> can be converted to Map<Object, String>
        assert objectStringMap.canBeConvertedFrom(intStringMap);

        //Map<String, Integer> cannot be converted to String
        assert !stringIntMap.canBeConvertedFrom(string);
        assert !string.canBeConvertedFrom(stringIntMap);
    }
}
