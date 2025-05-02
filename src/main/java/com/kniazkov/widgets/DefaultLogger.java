/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Default logger implementation that writes all messages to the standard system output.
 * <p>
 *     Each message is prefixed with a timestamp in the format {@code yyyy/MM/dd HH:mm:ss}.
 * </p>
 */
public final class DefaultLogger implements Logger {

    /**
     * Shared singleton instance of the logger.
     */
    public static final Logger INSTANCE = new DefaultLogger();

    /**
     * Date format used for timestamping log messages.
     */
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    /**
     * Private constructor to enforce singleton pattern.
     */
    private DefaultLogger() {
    }

    @Override
    public void write(final String message) {
        String timestamp = dateFormat.format(new Date());
        System.out.println(timestamp + " > " + message);
    }
}
