/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.model.Model;

/**
 * An editable text input field widget.
 */
public class InputField extends InlineWidget implements HasTextInput, HasStyledText, HasColor,
        HasBgColor, HasBorder, HasAbsoluteWidth, HasAbsoluteHeight, HasMargin,
        HasInvalidState, HasDisabledState, HandlesPointerEvents
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
     * Sets a new text model for this input field.
     * <p>
     * In addition to assigning the provided {@link Model} to {@link Property#TEXT},
     * this method also synchronizes the input field's validity state with the
     * validity of the underlying text data.
     * <p>
     * The supplied text model exposes its own validity flag via {@link Model#getValidFlagModel()}.
     * That flag is used to configure the field's {@code valid-state} model, ensuring that:
     * <ul>
     *   <li>If the text model contains invalid data, the widget immediately enters
     *       the invalid visual state.</li>
     *   <li>Subsequent validity changes in the text model automatically propagate
     *       to the widget's invalid-state flag.</li>
     * </ul>
     *
     * @param model the text model to assign
     */
    @Override
    public void setTextModel(Model<String> model) {
        this.setModel(State.ANY, Property.TEXT, model);
        this.setModel(State.ANY, Property.VALID, model.getValidFlagModel());
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
