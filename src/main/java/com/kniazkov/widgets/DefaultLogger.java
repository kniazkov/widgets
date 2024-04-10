/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jetbrains.annotations.NotNull;

/**
 * The logger that is used by default. It writes all logs to the system standard output.
 */
public final class DefaultLogger implements Logger {
    /**
     * Instance.
     */
    public static final Logger INSTANCE = new DefaultLogger();

    /**
     * Date format.
     */
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    /**
     * Constructor.
     */
    private DefaultLogger() {
    }

    @Override
    public void write(final @NotNull String message) {
        System.out.println(dateFormat.format(new Date()) + " > " + message);
    }
}
