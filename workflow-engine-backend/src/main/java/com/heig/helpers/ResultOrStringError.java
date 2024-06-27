package com.heig.helpers;

import com.heig.entities.workflow.errors.WorkflowErrors;

import java.util.function.Function;

public class ResultOrStringError<T> extends ResultOrError<T, String> {
    protected ResultOrStringError(T result, String errorMessage, boolean isError) {
        super(result, errorMessage, isError);
    }

    public static <U> ResultOrStringError<U> result(U result) {
        return new ResultOrStringError<>(result, null, false);
    }

    public static <U> ResultOrStringError<U> error(String errorMessage) {
        return new ResultOrStringError<>(null, errorMessage, true);
    }

    public <U> ResultOrStringError<U> continueWith(Function<T, ResultOrStringError<U>> funcResult) {
        return apply(funcResult, ResultOrStringError::error);
    }
}
