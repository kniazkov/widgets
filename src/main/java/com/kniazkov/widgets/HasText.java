/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Some entity that has a text.
 */
public interface HasText {
    /**
     * Returns a model that stores and processes the text of this entity.
     * @return A string model
     */
    @NotNull Model<String> getTextModel();

    /**
     * Sets the model that will store and process the text of this entity.
     * @param model A string model
     */
    void setTextModel(@NotNull Model<String> model);

    /**
     * Returns the current text.
     * @return Current text
     */
    default @NotNull String getText() {
        return this.getTextModel().getData();
    }

    /**
     * Sets the new text.
     * @param text Text
     */
    default void setText(@NotNull String text) {
        this.getTextModel().setData(text);
    }
}
