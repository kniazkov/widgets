/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * Defines the visual or interaction state of a widget.
 * States are used to determine which variant of a property model applies.
 */
public enum State {
    /**
     * Unknown state (meaning "the described entity is suitable for all widget states").
     */
    ANY("any"),

    /**
     * Normal state (default appearance).
     */
    NORMAL("normal"),

    /**
     * The pointer is hovering over the widget.
     */
    HOVERED("hovered"),

    /**
     * The widget is currently pressed or active.
     */
    ACTIVE("active"),

    /**
     * The widget content is invalid (e.g., failed validation).
     */
    INVALID("invalid"),

    /**
     * The widget is disabled and not interactive.
     */
    DISABLED("disabled");

    private final String key;

    State(final String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return this.key;
    }
}
