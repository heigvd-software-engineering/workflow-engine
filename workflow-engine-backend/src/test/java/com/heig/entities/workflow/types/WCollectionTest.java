package com.heig.entities.workflow.types;

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
        var integer = WPrimitive.Integer;
        var intList = WCollection.of(integer);
        var doubleList = WCollection.of(WPrimitive.Double);
        var obj = WObject.of();
        var objList = WCollection.of(WObject.of());

        //Collection<Integer> can be converted to Collection<Integer>
        assert intList.canBeConvertedFrom(intList);

        //Collection<Integer> can be converted to Object
        assert obj.canBeConvertedFrom(intList);
        //but a Collection cannot be converted from an object
        assert !intList.canBeConvertedFrom(obj);
        //nor a list of object
        assert !intList.canBeConvertedFrom(objList);

        //Collection<Integer> can be converted to Collection<Object>
        assert objList.canBeConvertedFrom(intList);

        //Collection<Integer> cannot be converted to Collection<Double>
        assert !doubleList.canBeConvertedFrom(intList);

        //Collection<Object> cannot be converted to Integer
        assert !integer.canBeConvertedFrom(objList);
        assert !objList.canBeConvertedFrom(integer);
    }
}
