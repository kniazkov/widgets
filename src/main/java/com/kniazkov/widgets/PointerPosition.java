/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Represents the position of a pointer (e.g., mouse or touch)
 * relative to different coordinate systems in a web or GUI environment.
 */
public final class PointerPosition {

    /**
     * The pointer's position relative to the target element.
     */
    public Point element;

    /**
     * The pointer's position relative to the visible portion of the viewport (client area).
     */
    public Point client;

    /**
     * The pointer's position relative to the entire document (page), including scroll offset.
     */
    public Point page;

    /**
     * The pointer's position relative to the physical screen.
     */
    public Point screen;
}
