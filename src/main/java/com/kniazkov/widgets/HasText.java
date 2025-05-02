/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * An entity that has an associated text value.
 * <p>
 *     This interface provides a contract for components (such as labels, buttons, inputs, etc.)
 *     that expose editable or displayable textual content. The text is managed via a
 *     {@link Model}{@code <String>}, enabling observation, data binding,
 *     and prototype-based behavior.
 * </p>
 */
public interface HasText {
    /**
     * Returns the model responsible for storing and processing the text of this entity.
     *
     * @return Model representing the current text
     */
    Model<String> getTextModel();

    /**
     * Sets the model that will manage the text of this entity.
     *
     * @param model Model that provides and accepts text
     */
    void setTextModel(Model<String> model);

    /**
     * Returns the current text from the model.
     * <p>
     *     Always returns a valid, non-null string.
     * </p>
     *
     * @return Current text
     */
    default String getText() {
        return this.getTextModel().getData();
    }

    /**
     * Updates the text value in the model.
     *
     * @param text New text to assign
     */
    default void setText(String text) {
        this.getTextModel().setData(text);
    }
}
