/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.protocol.Update;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Model;

/**
 * A {@link View} that has an associated text model.
 */
public interface HasText extends View {
    /**
     * Returns the model that stores the text for this view.
     *
     * @return the text model
     */
    Model<String> getTextModel();

    /**
     * Sets a new text model for this view.
     *
     * @param model the text model to set
     */
    void setTextModel(Model<String> model);

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
     * Listener that creates updates for the client in case of model changes.
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
            this.widget.update(update);
        }
    }
}
