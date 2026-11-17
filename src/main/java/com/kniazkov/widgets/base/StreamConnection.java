/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

/**
 * Authenticated physical channel. It does not assign event IDs or implement widget replay.
 * Application callbacks must still authorize each business operation.
 */
public interface StreamConnection extends AutoCloseable {
    /**
     * @return authenticated browser owner, not an application user identity
     */
    BrowserSession owner();

    /**
     * Offers a complete text message without waiting for the socket writer.
     * Acceptance is not a delivery or business acknowledgement. Overflow closes the stream.
     *
     * @param text UTF-8 application message
     * @return whether the still-authorized connection accepted the bounded write
     */
    boolean send(String text);

    /**
     * @return whether this physical channel remains authenticated and live
     */
    boolean isOpen();

    /**
     * Releases queues and cancels pending callbacks; cannot undo an executing business command.
     */
    @Override
    void close();
}
