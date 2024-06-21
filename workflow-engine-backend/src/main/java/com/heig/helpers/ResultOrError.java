package com.heig.helpers;

import com.heig.entities.workflow.errors.WorkflowErrors;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class ResultOrError<T> {
    private final T result;
    private final WorkflowErrors workflowErrors;
    private final boolean isError;

    private ResultOrError(T result, WorkflowErrors workflowErrors, boolean isError) {
        this.result = result;
        this.workflowErrors = workflowErrors;
        this.isError = isError;
    }

    public static <U> ResultOrError<U> result(U result) {
        return new ResultOrError<>(result, null, false);
    }

    public static <U> ResultOrError<U> error(WorkflowErrors workflowErrors) {
        return new ResultOrError<>(null, workflowErrors, true);
    }

    public Optional<T> getResult() {
        return !isError ? Optional.of(result) : Optional.empty();
    }

    public Optional<WorkflowErrors> getErrorMessage() {
        return isError ? Optional.of(workflowErrors) : Optional.empty();
    }

    public void executePresent(Consumer<T> consumerResult, Consumer<WorkflowErrors> consumerErrorMessage) {
        if (isError) {
            consumerErrorMessage.accept(workflowErrors);
        } else {
            consumerResult.accept(result);
        }
    }

    public <U> U applyPresent(Function<T, U> funcResult, Function<WorkflowErrors, U> funcErrorMessage) {
        if (isError) {
            return funcErrorMessage.apply(workflowErrors);
        } else {
            return funcResult.apply(result);
        }
    }
}
