/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

/**
 * A detachable listener registration.
 */
@FunctionalInterface
public interface Subscription extends AutoCloseable {
    /**
     * Detaches the listener.
     */
    @Override
    void close();
}
