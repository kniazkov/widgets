/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Represents the state of keyboard modifier keys during an input event,
 * such as mouse, keyboard, or pointer interactions.
 */
public class ModifierKeys {
    /**
     * {@code true} if the Control (Ctrl) key was pressed.
     */
    public boolean ctrl;

    /**
     * {@code true} if the Alt key was pressed.
     */
    public boolean alt;

    /**
     * {@code true} if the Shift key was pressed.
     */
    public boolean shift;

    /**
     * {@code true} if the Meta key was pressed (e.g., Command on macOS, Windows key on Windows).
     */
    public boolean meta;
}

