/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * Some entity that has a text.
 */
public interface HasText {
    /**
     * Returns a model that stores and processes the text of this entity.
     * @return A string model
     */
    @NotNull Model<String> getTextModel();

    /**
     * Sets the model that will store and process the text of this entity.
     * @param model A string model
     */
    void setTextModel(@NotNull Model<String> model);

    /**
     * Returns the current text.
     * @return Current text
     */
    default @NotNull String getText() {
        return this.getTextModel().getData();
    }

    /**
     * Sets the new text.
     * @param text Text
     */
    default void setText(@NotNull String text) {
        this.getTextModel().setData(text);
    }

    /**
     * Listener to follow model data updates and send instructions to customers.
     */
    class TextModelListener implements ModelListener<String> {
        /**
         * Widget containing model.
         */
        private final Widget widget;

        /**
         * Constructor.
         * @param widget Widget containing model
         */
        TextModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void dataChanged(final @NotNull String data) {
            final Instruction instruction = new Instruction(this.widget.getWidgetId()) {
                @Override
                protected @NotNull String getAction() {
                    return "set text";
                }

                @Override
                protected void fillJsonObject(final @NotNull JsonObject json) {
                    json.addString("text", data);
                }
            };
            this.widget.sendToClient(instruction);
        }
    }
}
