package com.heig.helpers;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class ResultOrError<T, E> {
    private final T result;
    private final E errors;
    private final boolean isError;

    protected ResultOrError(T result, E errors, boolean isError) {
        this.result = result;
        this.errors = errors;
        this.isError = isError;
    }

    public Optional<T> getResult() {
        return !isError ? Optional.of(result) : Optional.empty();
    }

    public Optional<E> getErrorMessage() {
        return isError ? Optional.of(errors) : Optional.empty();
    }

    public void execute(Consumer<T> consumerResult, Consumer<E> consumerErrorMessage) {
        if (isError) {
            consumerErrorMessage.accept(errors);
        } else {
            consumerResult.accept(result);
        }
    }

    public <U> U apply(Function<T, U> funcResult, Function<E, U> funcErrorMessage) {
        if (isError) {
            return funcErrorMessage.apply(errors);
        } else {
            return funcResult.apply(result);
        }
    }
}