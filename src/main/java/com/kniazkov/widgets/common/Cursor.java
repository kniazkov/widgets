/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Standard CSS cursor shapes useful for widgets.
 */
public enum Cursor {
    /**
     * Browser-selected cursor.
     */
    AUTO("auto"),
    /**
     * Default platform cursor.
     */
    DEFAULT("default"),
    /**
     * Link or clickable-item cursor.
     */
    POINTER("pointer"),
    /**
     * Text editing cursor.
     */
    TEXT("text"),
    /**
     * Operation is not permitted.
     */
    NOT_ALLOWED("not-allowed"),
    /**
     * Application is busy.
     */
    WAIT("wait"),
    /**
     * Operation is progressing in the background.
     */
    PROGRESS("progress"),
    /**
     * Contextual help is available.
     */
    HELP("help"),
    /**
     * Element can be moved.
     */
    MOVE("move"),
    /**
     * Element can be grabbed.
     */
    GRAB("grab"),
    /**
     * Element is being grabbed.
     */
    GRABBING("grabbing"),
    /**
     * Crosshair cursor.
     */
    CROSSHAIR("crosshair"),
    /**
     * Cursor is hidden.
     */
    NONE("none");

    /**
     * CSS keyword.
     */
    private final String cssCode;

    /**
     * Creates a cursor value.
     *
     * @param cssCode CSS keyword
     */
    Cursor(final String cssCode) {
        this.cssCode = cssCode;
    }

    /**
     * Returns the CSS keyword.
     *
     * @return CSS cursor value
     */
    public String getCSSCode() {
        return this.cssCode;
    }

    @Override
    public String toString() {
        return this.cssCode;
    }
}
