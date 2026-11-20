/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * A control that can declare an accessible name independently of its visible appearance.
 */
public interface HasAccessibleName extends Entity {
    /**
     * Returns the model storing the optional accessible name.
     * @return accessible name model
     */
    default Model<String> getAccessibleNameModel() {
        return this.getModel(State.ANY, Property.ACCESSIBLE_NAME);
    }

    /**
     * Binds a model as the accessible name.
     * @param model name model
     */
    default void setAccessibleNameModel(final Model<String> model) {
        this.setModel(State.ANY, Property.ACCESSIBLE_NAME, model);
    }

    /**
     * Returns the explicitly assigned name, which may be empty.
     * @return assigned name
     */
    default String getAccessibleName() {
        return this.getAccessibleNameModel().getData();
    }

    /**
     * Assigns an accessible name; an empty string restores the visible label or fallback.
     * @param name accessible name
     */
    default void setAccessibleName(final String name) {
        this.getAccessibleNameModel().setData(name);
    }
}
