package com.heig.entities.workflow.types;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WPrimitiveTest {
    @Test
    public void compatibility() {
        var object = WObject.of();
        var string = WPrimitive.String;
        var integer = WPrimitive.Integer;
        var doubleT = WPrimitive.Double;

        //String can be converted to String
        assert string.canBeConvertedFrom(string);

        //String cannot be converted to Integer
        assert !string.canBeConvertedFrom(integer);
        assert !integer.canBeConvertedFrom(string);

        //Double cannot be converted to Integer
        assert !doubleT.canBeConvertedFrom(integer);
        assert !integer.canBeConvertedFrom(doubleT);

        //Integer can be converted to Object
        assert object.canBeConvertedFrom(integer);
        //but an Integer cannot be converted from an object
        assert !integer.canBeConvertedFrom(object);
    }
}
