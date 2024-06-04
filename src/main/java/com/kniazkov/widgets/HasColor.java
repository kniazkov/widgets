/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Some entity that has a color.
 */
public interface HasColor {
    /**
     * Returns a model that stores and processes the color of this entity.
     * @return A color model
     */
    @NotNull Model<Color> getColorModel();

    /**
     * Sets the model that will store and process the color of this entity.
     * @param model A color model
     */
    void setColorModel(@NotNull Model<Color> model);

    /**
     * Returns the current color.
     * @return Current color
     */
    default @NotNull Color getColor() {
        return this.getColorModel().getData();
    }

    /**
     * Sets the new color.
     * @param color Text
     */
    default void setColor(@NotNull Color color) {
        this.getColorModel().setData(color);
    }
}
