/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An entity with a reactive placeholder property.
 */
public interface HasPlaceholder extends Entity {
    /**
     * @return property model
     */
    default Model<String> getPlaceholderModel() {
        return this.getModel(State.ANY, Property.PLACEHOLDER);
    }
    /**
     * @param model replacement property model
     */
    default void setPlaceholderModel(final Model<String> model) {
        this.setModel(State.ANY, Property.PLACEHOLDER, model);
    }
    /**
     * @return current property value
     */
    default String getPlaceholder() {
        return this.getPlaceholderModel().getData();
    }
    /**
     * @param value new property value
     */
    default void setPlaceholder(final String value) {
        this.getPlaceholderModel().setData(value);
    }
}
