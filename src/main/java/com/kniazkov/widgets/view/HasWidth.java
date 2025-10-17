/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.WidgetSize;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;
import com.kniazkov.widgets.model.ModelListener;
import com.kniazkov.widgets.protocol.SetWidth;

/**
 * A {@link View} that has an associated width model.
 *
 * @param <T> the specific type of {@link WidgetSize} used by the view
 */
public interface HasWidth<T extends WidgetSize> extends View {
    /**
     * Returns the model binding associated with the specified property.
     *
     * @param property the property key
     * @return the model binding associated with the given property
     */
    <Q> ModelBinding<Q> getBinding(Property property);

    /**
     * Returns the binding that connects the width model to this widget.
     *
     * @return the width model binding
     */
    default ModelBinding<T> getWidthModelBinding() {
        return this.getBinding(Property.WIDTH);
    }

    /**
     * Returns the model that stores the width for this view.
     *
     * @return the width model
     */
    default Model<T> getWidthModel() {
        return this.getWidthModelBinding().getModel();
    }

    /**
     * Sets a new width model for this view.
     *
     * @param model the width model to set
     */
    default void setWidthModel(final Model<T> model) {
        this.getWidthModelBinding().setModel(model);
    }

    /**
     * Returns the current width from the current model.
     *
     * @return the current width
     */
    default T getWidth() {
        return this.getWidthModel().getData();
    }

    /**
     * Updates the width in the model.
     *
     * @param width the new width
     */
    default void setWidth(final T width) {
        this.getWidthModel().setData(width);
    }

    /**
     * Listener that listens to width models and sends a "set width" update.
     */
    final class WidthModelListener<T extends WidgetSize> extends ModelListener<T> {
        /**
         * Creates a new listener bound to the specified target.
         *
         * @param target the target whose width will be updated
         */
        public WidthModelListener(final Entity target) {
            super(target);
        }

        @Override
        public void accept(final T data) {
            this.getTarget().pushUpdate(new SetWidth<T>(this.getTarget().getId(), data));
        }

        @Override
        public ModelListener<T> create(final Entity target) {
            return new WidthModelListener<>(target);
        }
    }
}
