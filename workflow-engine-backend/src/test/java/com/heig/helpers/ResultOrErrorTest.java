package com.heig.helpers;

import com.heig.entities.workflow.errors.WorkflowErrors;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Objects;

@QuarkusTest
public class ResultOrErrorTest {
    @Test
    public void test() {
        //ResultOrError with errors
        var errors = new WorkflowErrors();
        var result1 = ResultOrError.error(errors);
        assert result1.getResult().isEmpty();
        assert result1.getErrorMessage().isPresent();
        result1.executePresent(r -> { assert false; }, e -> { assert true; });
        assert Objects.equals(result1.getErrorMessage().get(), errors);

        //ResultOrError with integer value
        var resultInt = 122;
        var result2 = ResultOrError.result(resultInt);
        assert result2.getErrorMessage().isEmpty();
        assert result2.getResult().isPresent();
        result2.executePresent(r -> { assert true; }, e -> { assert false; });
        assert result2.getResult().get() == resultInt;
    }
}
