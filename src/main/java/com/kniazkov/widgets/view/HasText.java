/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.SetText;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;
import com.kniazkov.widgets.model.ModelListener;

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
    <T> ModelBinding<T> getBinding(Property property);

    /**
     * Returns the binding that connects the text model to this widget.
     *
     * @return the text model binding
     */
    default ModelBinding<String> getTextModelBinding() {
        return this.getBinding(Property.TEXT);
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
    final class TextModelListener extends ModelListener<String> {
        /**
         * Creates a new listener bound to the specified target.
         *
         * @param target the target whose text will be updated
         */
        public TextModelListener(final Entity target) {
            super(target);
        }

        @Override
        public void accept(final String data) {
            this.getTarget().pushUpdate(new SetText(this.getTarget().getId(), data));
        }

        @Override
        public ModelListener<String> create(final Entity target) {
            return new TextModelListener(target);
        }
    }
}
