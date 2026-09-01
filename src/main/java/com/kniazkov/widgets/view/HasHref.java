/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that exposes a reactive hyperlink destination.
 */
public interface HasHref extends Entity {
    /**
     * Returns the model that stores the hyperlink destination.
     *
     * @return hyperlink destination model
     */
    default Model<String> getHrefModel() {
        return this.getModel(State.ANY, Property.HREF);
    }

    /**
     * Sets the model that stores the hyperlink destination.
     *
     * @param model hyperlink destination model
     */
    default void setHrefModel(final Model<String> model) {
        this.setModel(State.ANY, Property.HREF, model);
    }

    /**
     * Returns the current hyperlink destination.
     *
     * @return hyperlink destination
     */
    default String getHref() {
        return this.getHrefModel().getData();
    }

    /**
     * Updates the hyperlink destination.
     *
     * @param href new hyperlink destination
     */
    default void setHref(final String href) {
        this.getHrefModel().setData(href);
    }
}
