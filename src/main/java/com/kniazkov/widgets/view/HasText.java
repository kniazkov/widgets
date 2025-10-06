/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.protocol.Update;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;

/**
 * A {@link View} that has an associated text model.
 */
public interface HasText extends View {
    /**
     * Returns the binding that connects the text model to this widget.
     *
     * @return the text model binding
     */
    ModelBinding<String> getTextModelBinding();

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
            final Update update = new Update(this.widget.getId()) {
                @Override
                protected String getAction() {
                    return "set text";
                }

                @Override
                protected void fillJsonObject(JsonObject json) {
                    json.addString("text", data);
                }
            };
            this.widget.pushUpdate(update);
        }
    }
}
