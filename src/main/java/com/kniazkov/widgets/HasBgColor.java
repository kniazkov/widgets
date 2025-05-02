/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

package com.kniazkov.widgets;

/**
 * An entity that has a background color.
 * <p>
 *     This interface defines access to a background {@link Color} managed through a {@link Model}.
 *     It is designed to coexist with {@link HasColor}, allowing components to expose both
 *     foreground and background color separately.
 * </p>
 */
public interface HasBgColor {
    /**
     * Returns the model responsible for storing and managing the background color.
     *
     * @return Model containing the background color
     */
    Model<Color> getBackgroundColorModel();

    /**
     * Sets the model responsible for storing and managing the background color.
     *
     * @param model Model that provides the background color
     */
    void setBackgroundColorModel(Model<Color> model);

    /**
     * Returns the current background color from the model.
     *
     * @return Background color
     */
    default Color getBackgroundColor() {
        return this.getBackgroundColorModel().getData();
    }

    /**
     * Sets a new background color via the model.
     *
     * @param color Background color to assign
     */
    default void setBackgroundColor(Color color) {
        this.getBackgroundColorModel().setData(color);
    }
}
