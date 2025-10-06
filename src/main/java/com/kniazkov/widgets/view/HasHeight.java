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
 * A {@link View} that has an associated height model.
 *
 * @param <T> the specific type of {@link WidgetSize} used by the view
 */
public interface HasHeight<T extends WidgetSize> extends View {

    /**
     * Returns the binding that connects the height model to this widget.
     *
     * @return the height model binding
     */
    ModelBinding<T> getHeightModelBinding();

    /**
     * Returns the model that stores the height for this view.
     *
     * @return the height model
     */
    default Model<T> getHeightModel() {
        return this.getHeightModelBinding().getModel();
    }

    /**
     * Sets a new height model for this view.
     *
     * @param model the height model to set
     */
    default void setHeightModel(final Model<T> model) {
        this.getHeightModelBinding().setModel(model);
    }

    /**
     * Returns the current height from the current model.
     *
     * @return the current height
     */
    default T getHeight() {
        return this.getHeightModel().getData();
    }

    /**
     * Updates the height in the model.
     *
     * @param height the new height
     */
    default void setHeight(final T height) {
        this.getHeightModel().setData(height);
    }

    /**
     * Listener that listens to height models and sends a "set height" update.
     */
    final class HeightModelListener<T extends WidgetSize> implements Listener<T> {
        private final Widget widget;

        /**
         * Creates a new listener bound to the specified widget.
         *
         * @param widget the widget whose height will be updated
         */
        public HeightModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void accept(final T data) {
            final Update update = new Update(this.widget.getId()) {
                @Override
                protected String getAction() {
                    return "set height";
                }

                @Override
                protected void fillJsonObject(final JsonObject json) {
                    json.addString("height", data.getCSSCode());
                }
            };
            this.widget.pushUpdate(update);
        }
    }
}
