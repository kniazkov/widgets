/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;
import com.kniazkov.widgets.model.ModelListener;
import com.kniazkov.widgets.protocol.SetBgColor;

/**
 * A {@link View} that has an associated background color model.
 */
public interface HasBgColor extends View {
    /**
     * Returns a state-dependent binding for the specified property and state.
     *
     * @param property property key
     * @param state widget state
     * @param <T> binding data type
     * @return the model binding
     * @throws IllegalStateException if no binding is found for the given state
     */
    <T> ModelBinding<T> getBinding(Property property, WidgetState state);

    /**
     * Returns the binding that connects the background color model to this widget.
     *
     * @return the background color model binding
     */
    default ModelBinding<Color> getBgColorModelBinding() {
        return this.getBinding(Property.BG_COLOR, WidgetState.NORMAL);
    }

    /**
     * Returns the model that stores the background color for this view.
     *
     * @return the background color model
     */
    default Model<Color> getBgColorModel() {
        return this.getBgColorModelBinding().getModel();
    }

    /**
     * Sets a new background color model for this view.
     *
     * @param model the background color model to set
     */
    default void setBgColorModel(final Model<Color> model) {
        this.getBgColorModelBinding().setModel(model);
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
    final class BgColorModelListener extends ModelListener<Color> {
        /**
         * Creates a new listener bound to the specified target.
         *
         * @param target the target whose background color will be updated
         */
        public BgColorModelListener(final Entity target) {
            super(target);
        }

        @Override
        public void accept(final Color data) {
            this.getTarget().pushUpdate(new SetBgColor(this.getTarget().getId(), data));
        }

        @Override
        public ModelListener<Color> create(final Entity target) {
            return new BgColorModelListener(target);
        }
    }
}
