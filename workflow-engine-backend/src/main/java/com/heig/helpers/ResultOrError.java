package com.heig.helpers;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Custom class representing either a result or an error
 * @param <T> The type of the value
 * @param <E> The type of the error
 */
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

    /**
     * Executes the {@link Consumer} linked to the current state
     * @param consumerResult Executed if not errored
     * @param consumerError Executed if errored
     */
    public void execute(Consumer<T> consumerResult, Consumer<E> consumerError) {
        if (isError) {
            consumerError.accept(errors);
        } else {
            consumerResult.accept(result);
        }
    }

    /**
     * Returns the result of a {@link Function} linked to the current state
     * @param funcResult Function if not errored
     * @param funcError Function if errored
     * @return The result of funcResult if not errored, funcError otherwise
     * @param <U> The type of the functions result
     */
    public <U> U apply(Function<T, U> funcResult, Function<E, U> funcError) {
        if (isError) {
            return funcError.apply(errors);
        } else {
            return funcResult.apply(result);
        }
    }
}