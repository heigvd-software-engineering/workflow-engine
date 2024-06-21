package com.heig.helpers;

import com.heig.entities.workflow.errors.WorkflowErrors;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@QuarkusTest
public class ResultOrErrorTest {
    @Test
    public void test() {
        //ResultOrError with errors
        var errors = new WorkflowErrors();
        var result1 = ResultOrError.error(errors);
        assert result1.getResult().isEmpty();
        assert result1.getErrorMessage().isPresent();
        assert result1.applyPresent(r -> false, e -> true);
        assert Objects.equals(result1.getErrorMessage().get(), errors);

        //ResultOrError with integer value
        var resultInt = 122;
        var result2 = ResultOrError.result(resultInt);
        assert result2.getErrorMessage().isEmpty();
        assert result2.getResult().isPresent();
        var resultAssert = new AtomicBoolean(false);
        result2.executePresent(r -> resultAssert.set(true), e -> resultAssert.set(false));
        assert resultAssert.get();
        assert result2.getResult().get() == resultInt;
    }
}
