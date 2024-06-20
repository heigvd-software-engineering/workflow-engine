package com.heig.entities.workflow.types;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WObjectTest {
    @Test
    public void onlyOneInstance() {
        assert WObject.of() == WObject.of();
    }

    @Test
    public void compatibility() {
        var obj = WObject.of();

        //Object can be converted to Object
        assert obj.canBeConvertedFrom(obj);
    }
}
