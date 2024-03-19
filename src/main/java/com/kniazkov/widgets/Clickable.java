/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Some entity you can click on.
 */
public interface Clickable {
    /**
     * Sets the controller that determines the behavior when an entity is clicked.
     * @param ctrl Controller
     */
    void onClick(Controller ctrl);
}
