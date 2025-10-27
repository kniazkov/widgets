/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.ReadOnlyModel;
import java.util.Optional;

/**
 * A simple text widget that displays styled textual content.
 */
public class TextWidget extends InlineWidget implements HasStyledText, HasColor {
    /**
     * Returns the default style instance used by text widgets.
     *
     * @return the singleton default {@link TextWidgetStyle} instance
     */
    public static TextWidgetStyle getDefaultStyle() {
        return DefaultStyle.INSTANCE;
    }

    /**
     * Creates a new text widget with empty text.
     */
    public TextWidget() {
        this("");
    }

    /**
     * Creates a new text widget with the given initial text value and the default style.
     *
     * @param text the initial text to display
     */
    public TextWidget(final String text) {
        this(DefaultStyle.INSTANCE, text);
    }

    /**
     * Creates a new text widget with the specified style and initial text.
     *
     * @param style the style to apply to this widget
     * @param text  the initial text to display
     */
    public TextWidget(final TextWidgetStyle style, final String text) {
        super(style);
        this.bindData(State.NORMAL, Property.TEXT, text);
    }

    @Override
    public String getType() {
        return "text";
    }

    @Override
    public void handleEvent(final String type, final Optional<JsonObject> data) {
        // no events to process
    }

    /**
     * The default style implementation for {@link TextWidget}.
     */
    private static final class DefaultStyle extends TextWidgetStyle {

        /**
         * Singleton instance of the default style.
         */
        public static final TextWidgetStyle INSTANCE = new DefaultStyle();

        /**
         * Initializes the default text style configuration.
         */
        private DefaultStyle() {
            this.bindModel(State.ANY, Property.TEXT, ReadOnlyModel.create(""));
            this.bindData(State.NORMAL, Property.COLOR, Color.BLACK);
        }
    }
}
