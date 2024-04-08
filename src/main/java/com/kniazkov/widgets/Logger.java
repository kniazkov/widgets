/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for logging server messages.
 */
public interface Logger {
    /**
     * Writes a message to the log.
     * @param message Message
     */
    void write(@NotNull String message);
}
