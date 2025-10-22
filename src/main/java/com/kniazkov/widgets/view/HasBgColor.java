package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.Binding;
import com.kniazkov.widgets.protocol.SetBgColor;

/**
 * A {@link View} that has an associated background color model.
 */
public interface HasBgColor extends View {
    /**
     * Returns the binding that connects the background color model to this widget.
     *
     * @return the background color model binding
     */
    Binding<Color> getBgColorModelBinding();

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
    final class BgColorModelListener implements Listener<Color> {
        private final Widget widget;

        /**
         * Creates a new listener bound to the specified widget.
         *
         * @param widget the widget whose background color will be updated
         */
        public BgColorModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void accept(final Color data) {
            this.widget.pushUpdate(new SetBgColor(this.widget.getId(), data));
        }
    }
}
