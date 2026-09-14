/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * An interactive widget that can open a configured hyperlink in a new browser tab directly
 * from its client-side click handler.
 *
 * <p>The direct browser-side action preserves the user gesture required by mobile popup
 * blockers. An empty hyperlink disables the action.</p>
 */
public interface HasNewTabHref extends Entity {
    /**
     * Returns the model containing the hyperlink opened by a click.
     *
     * @return new-tab hyperlink model
     */
    default Model<String> getNewTabHrefModel() {
        return this.getModel(State.ANY, Property.NEW_TAB_HREF);
    }

    /**
     * Replaces the model containing the hyperlink opened by a click.
     *
     * @param model new-tab hyperlink model
     */
    default void setNewTabHrefModel(final Model<String> model) {
        this.setModel(State.ANY, Property.NEW_TAB_HREF, model);
    }

    /**
     * Returns the hyperlink opened by a click.
     *
     * @return hyperlink, or an empty string when direct opening is disabled
     */
    default String getNewTabHref() {
        return this.getNewTabHrefModel().getData();
    }

    /**
     * Sets the hyperlink opened by a click.
     *
     * @param href hyperlink, or an empty string to disable direct opening
     */
    default void setNewTabHref(final String href) {
        this.getNewTabHrefModel().setData(href);
    }
}
