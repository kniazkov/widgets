package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.protocol.SetColor;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;

/**
 * A {@link View} that has an associated color model.
 */
public interface HasColor extends View {
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
     * Returns the binding that connects the color model to this widget.
     *
     * @return the color model binding
     */
    default ModelBinding<Color> getColorModelBinding() {
        return this.getBinding(Property.COLOR, WidgetState.NORMAL);
    }

    /**
     * Returns the model that stores the color for this view.
     *
     * @return the color model
     */
    default Model<Color> getColorModel() {
        return this.getColorModelBinding().getModel();
    }

    /**
     * Sets a new color model for this view.
     *
     * @param model the color model to set
     */
    default void setColorModel(final Model<Color> model) {
        this.getColorModelBinding().setModel(model);
    }

    /**
     * Returns the current color from the current model.
     *
     * @return the current color
     */
    default Color getColor() {
        return this.getColorModel().getData();
    }

    /**
     * Updates the color in the model.
     *
     * @param color the new color
     */
    default void setColor(final Color color) {
        this.getColorModel().setData(color);
    }

    /**
     * Listener that listens to color models and sends a "set color" update.
     */
    final class ColorModelListener implements Listener<Color> {
        private final Widget widget;

        /**
         * Creates a new listener bound to the specified widget.
         *
         * @param widget the widget whose color will be updated
         */
        public ColorModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void accept(final Color data) {
            this.widget.pushUpdate(new SetColor(this.widget.getId(), data));
        }
    }
}
