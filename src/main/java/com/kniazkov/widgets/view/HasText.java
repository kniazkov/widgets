/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.protocol.SetText;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.Binding;

/**
 * A {@link View} that has an associated text model.
 */
public interface HasText extends View {
    /**
     * Returns the model binding associated with the specified property.
     *
     * @param property the property key
     * @return the model binding associated with the given property
     */
    <T> Binding<T> getModelBinding(final Property property);

    /**
     * Returns the binding that connects the text model to this widget.
     *
     * @return the text model binding
     */
    default Binding<String> getTextModelBinding() {
        return this.getModelBinding(Property.TEXT);
    }

    /**
     * Returns the model that stores the text for this view.
     *
     * @return the text model
     */
    default Model<String> getTextModel() {
        return this.getTextModelBinding().getModel();
    }

    /**
     * Sets a new text model for this view.
     *
     * @param model the text model to set
     */
    default void setTextModel(Model<String> model) {
        this.getTextModelBinding().setModel(model);
    }

    /**
     * Returns the current text from the current model.
     *
     * @return the current text
     */
    default String getText() {
        return this.getTextModel().getData();
    }

    /**
     * Updates the text in the model.
     *
     * @param text the new text
     */
    default void setText(String text) {
        this.getTextModel().setData(text);
    }

    /**
     * Listener that listens to text models and sends a "set text" update.
     */
    final class TextModelListener implements Listener<String> {
        private final Widget widget;

        public TextModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void accept(final String data) {
            this.widget.pushUpdate(new SetText(this.widget.getId(), data));
        }
    }
}
