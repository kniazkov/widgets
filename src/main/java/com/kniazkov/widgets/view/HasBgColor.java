/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.protocol.SetBgColor;

/**
 * An {@link Entity} that has an associated background color model.
 */
public interface HasBgColor extends Entity {
    /**
     * Returns the model that stores the background color for this view.
     *
     * @return the background color model
     */
    default Model<Color> getBgColorModel() {
        return this.getModel(State.NORMAL, Property.BG_COLOR, Color.class);
    }

    /**
     * Sets a new background color model for this view.
     *
     * @param model the background color model to set
     */
    default void setBgColorModel(final Model<Color> model) {
        this.setModel(State.NORMAL, Property.BG_COLOR, Color.class, model);
    }

    /**
     * Returns the current background color from the current model.
     *
     * @return the current background color
     */
    default Color getBgColor() {
        return this.getBgColorModel().getData();
    }

    /**
     * Updates the background color in the model.
     *
     * @param color the new background color
     */
    default void setBgColor(final Color color) {
        this.getBgColorModel().setData(color);
    }

    /**
     * Listener that listens to background color models and sends a "set background color" update.
     */
    final class BgColorModelListener implements Listener<Color> {
        private final Widget widget;
        private final State state;

        /**
         * Creates a new listener bound to the specified widget.
         *
         * @param widget the widget whose background color will be updated
         * @param state the state of the widget in which the change is applied
         */
        public BgColorModelListener(final Widget widget, final State state) {
            this.widget = widget;
            this.state = state;
        }

        @Override
        public void accept(final Color data) {
            this.widget.pushUpdate(new SetBgColor(this.widget.getId(), this.state, data));
        }
    }
}
