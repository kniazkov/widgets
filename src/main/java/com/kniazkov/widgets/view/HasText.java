/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has an associated text model.
 */
public interface HasText extends Entity {
    /**
     * Returns the model that stores the text for this view.
     *
     * @return the text model
     */
    default Model<String> getTextModel() {
        return this.getModel(State.ANY, Property.TEXT, String.class);
    }

    /**
     * Sets a new text model for this view.
     *
     * @param model the text model to set
     */
    default void setTextModel(Model<String> model) {
        this.setModel(State.ANY, Property.TEXT, String.class, model);
    }

    /**
     * Returns the current text from the current model.
     *
     * @return the current text
     */
    default String getText() {
        return this.getTextModel().getData();
    }

    /**
     * Updates the text in the model.
     *
     * @param text the new text
     */
    default void setText(String text) {
        this.getTextModel().setData(text);
    }
}
