/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A boolean-valued function of one argument.
 *
 * @param <T> The type of the input to the predicate
 */
public interface Predicate<T> {
    /**
     * Evaluates this predicate on the given input.
     *
     * @param input The input argument
     * @return {@code true} If the input matches the condition, otherwise {@code false}
     */
    boolean test(T input);
}
