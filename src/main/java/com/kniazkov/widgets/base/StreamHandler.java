/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

/**
 * Receives bounded authenticated messages on a dedicated executor, never under a widget lock.
 * Callbacks for one connection are serialized. Do not block indefinitely or ignore interruption.
 */
@FunctionalInterface
public interface StreamHandler {
    /**
     * @param connection authenticated physical connection
     * @param message application text, after the authentication frame
     * @throws Exception failure closes this connection without exposing exception details
     */
    void onText(StreamConnection connection, String message) throws Exception;
}
