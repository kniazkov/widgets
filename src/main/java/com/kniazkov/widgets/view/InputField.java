/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.PointerEvent;
import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.controller.Controller;
import java.util.Optional;

/**
 * An editable text input field widget.
 */
public class InputField extends InlineWidget implements HasTextInput,
        HasStyledText, HasHoverStyledText, HasFocusStyledText, HasDisabledStyledText, HasInvalidStyledText,
        HasColor, HasHoverColor, HasFocusColor, HasDisabledColor, HasInvalidColor,
        HasBgColor, HasHoverBgColor, HasFocusBgColor, HasDisabledBgColor, HasInvalidBgColor,
        HasAbsoluteWidth, HasAbsoluteHeight, HasMargin,
        HandlesPointerEvents
{
    /**
     * Returns the default style instance used by input fields.
     *
     * @return the singleton default {@link TextWidgetStyle} instance
     */
    public static InputFieldStyle getDefaultStyle() {
        return InputFieldStyle.DEFAULT;
    }

    /**
     * Creates a new input field with empty text.
     */
    public InputField() {
        this("");
    }

    /**
     * Creates a new input field with the given initial text.
     *
     * @param text the initial text to display in the input field
     */
    public InputField(final String text) {
        this(getDefaultStyle(), text);
    }

    /**
     * Creates a new input field with the specified style and initial text.
     *
     * @param style the style to apply to this widget
     * @param text the initial text to display
     */
    public InputField(final InputFieldStyle style, final String text) {
        super(style);
        this.setText(text);
    }

    @Override
    public String getType() {
        return "input field";
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    public void setStyle(InputFieldStyle style) {
        super.setStyle(style);
    }
}
