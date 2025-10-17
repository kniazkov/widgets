/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.model.DefaultStringModel;
import java.util.Optional;

/**
 * A simple text widget.
 */
public class TextWidget extends InlineWidget implements HasStyledText, HasColor {
    /**
     * Creates a new text widget with empty text.
     */
    public TextWidget() {
        this("");
    }

    /**
     * Creates a new text widget with the given initial text.
     *
     * @param text the initial text
     */
    public TextWidget(final String text) {
        this.setStyle(TextWidgetStyle.DEFAULT);
        this.bind(
            Property.TEXT,
            new DefaultStringModel(text),
            new TextModelListener(this),
            DefaultStringModel.FACTORY
        );
    }

    /**
     * Sets the style of the text widget.
     *
     * @param style new style of text widget
     */
    public void setStyle(final TextWidgetStyle style) {
        super.setStyle(style);
    }

    @Override
    public String getType() {
        return "text";
    }

    @Override
    public void handleEvent(String type, Optional<JsonObject> data) {
        // no events to process
    }
}
