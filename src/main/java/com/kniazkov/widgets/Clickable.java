/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Some entity you can click on.
 */
public interface Clickable {
    /**
     * Sets the controller that determines the behavior when an entity is clicked.
     * @param ctrl Controller
     */
    void onClick(@NotNull Controller ctrl);
}
