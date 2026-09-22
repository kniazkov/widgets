/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.controller;

/**
 * A versioned request to move a child before another child (or to the end).
 */
public final class ReorderEvent {
    /**
     * Creates an empty event for JSON deserialization.
     */
    public ReorderEvent() {
    }

    /**
     * Container revision seen before the gesture.
     */
    public int revision;

    /**
     * Identifier of the moved child.
     */
    public String child;

    /**
     * Identifier of the next child; an empty string means the end.
     */
    public String before;
}
