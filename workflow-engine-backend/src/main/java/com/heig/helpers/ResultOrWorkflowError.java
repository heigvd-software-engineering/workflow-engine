package com.heig.helpers;

import com.heig.entities.workflow.errors.WorkflowErrors;

/**
 * Implementation of {@link ResultOrError} with a {@link WorkflowErrors} as an error type
 * @param <T> The result type
 */
public class ResultOrWorkflowError<T> extends ResultOrError<T, WorkflowErrors> {
    protected ResultOrWorkflowError(T result, WorkflowErrors errors, boolean isError) {
        super(result, errors, isError);
    }

    public static <U> ResultOrWorkflowError<U> result(U result) {
        return new ResultOrWorkflowError<>(result, null, false);
    }

    public static <U> ResultOrWorkflowError<U> error(WorkflowErrors workflowErrors) {
        return new ResultOrWorkflowError<>(null, workflowErrors, true);
    }
}
