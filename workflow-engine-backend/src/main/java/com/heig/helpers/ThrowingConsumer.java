package com.heig.helpers;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {
    void accept(final T elem) throws E;
}
