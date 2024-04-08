/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * The logger that is used by default. It writes all logs to the system standard output.
 */
public class DefaultLogger implements Logger {
    /**
     * Instance.
     */
    public static final Logger INSTANCE = new DefaultLogger();

    /**
     * Constructor.
     */
    private DefaultLogger() {
    }

    @Override
    public void write(final @NotNull String message) {
        System.out.println(message);
    }
}
