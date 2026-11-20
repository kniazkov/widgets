/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

/**
 * Lifecycle and negotiated frame budget of the optional push listener.
 */
interface PushService {
    /**
     * @return whether the listener has stopped
     */
    boolean isClosed();

    /**
     * @return maximum advertised frame size
     */
    int messageBytes();
}
