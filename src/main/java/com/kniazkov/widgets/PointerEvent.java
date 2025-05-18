/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Represents a pointer input event, such as from a mouse, touch, or pen device.
 * Contains position data, input type, button state, modifier keys, and pressure information.
 */
public class PointerEvent {
    /**
     * A no-op (stub) controller for {@link PointerEvent}.
     * <p>
     *     This implementation does nothing when invoked and can be used as a default
     *     or placeholder.
     * </p>
     */
    public static final TypedController<PointerEvent> STUB_CONTROLLER = data -> {
        // do nothing
    };

    /**
     * The pointer's position relative to various coordinate systems.
     */
    public PointerPosition position;

    /**
     * The type of pointer device (e.g. mouse, touch, pen).
     */
    public String type;

    /**
     * Indicates whether this pointer is the primary pointer in a multi-pointer scenario.
     */
    public boolean primary;

    /**
     * A bitfield representing the mouse or pointer buttons currently pressed.
     * For example, 1 = left, 2 = right, 4 = middle.
     */
    public int buttons;

    /**
     * The state of modifier keys (Ctrl, Alt, Shift, Meta) at the time of the event.
     */
    public ModifierKeys keys;

    /**
     * The pressure of the pointer input, ranging from 0.0 (no pressure) to 1.0 (maximum pressure).
     * Typically used with stylus or touch devices.
     */
    public double pressure;
}
