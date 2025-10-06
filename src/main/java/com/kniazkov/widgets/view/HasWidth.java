/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.common.WidgetSize;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;
import com.kniazkov.widgets.protocol.Update;

/**
 * A {@link View} that has an associated width model.
 *
 * @param <T> the specific type of {@link WidgetSize} used by the view
 */
public interface HasWidth<T extends WidgetSize> extends View {

    /**
     * Returns the binding that connects the width model to this widget.
     *
     * @return the width model binding
     */
    ModelBinding<T> getWidthModelBinding();

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
    final class WidthModelListener<T extends WidgetSize> implements Listener<T> {
        private final Widget widget;

        /**
         * Creates a new listener bound to the specified widget.
         *
         * @param widget the widget whose width will be updated
         */
        public WidthModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void accept(final T data) {
            final Update update = new Update(this.widget.getId()) {
                @Override
                protected String getAction() {
                    return "set width";
                }

                @Override
                protected void fillJsonObject(final JsonObject json) {
                    json.addString("width", data.getCSSCode());
                }
            };
            this.widget.pushUpdate(update);
        }
    }
}
