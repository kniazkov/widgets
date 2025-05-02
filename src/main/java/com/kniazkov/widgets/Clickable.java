/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * An entity that can be clicked by the user.
 * <p>
 *     This interface represents interactive elements such as buttons, icons, or clickable text.
 *     It allows associating a {@link Controller} that defines the behavior to execute
 *     when a click event occurs.
 * </p>
 */
public interface Clickable {
    /**
     * Sets a controller that will be invoked when the entity is clicked.
     *
     * @param ctrl The controller to run on click
     */
    void onClick(Controller ctrl);
}
