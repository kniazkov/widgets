/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;

/**
 * A RadioButton with a caption on its right. Clicking the caption selects the control.
 * Disabled state is shared by the control and caption; the caption becomes gray.
 */
public final class RadioButtonWithText extends LabeledChoice<RadioButton> {
    /**
     * Creates an unchecked control with an empty caption.
     */
    public RadioButtonWithText() {
        this("");
    }

    /**
     * Creates an unchecked control with the supplied caption.
     * @param text initial caption
     */
    public RadioButtonWithText(final String text) {
        this(new StringModel(text), new BooleanModel(false));
    }

    /**
     * Binds existing caption and selection models directly.
     * @param text caption model
     * @param checked selection model
     */
    public RadioButtonWithText(final Model<String> text, final Model<Boolean> checked) {
        super(new RadioButton(), text, checked, true);
    }

    /**
     * Returns the contained RadioButton for customization or RadioGroup membership.
     * @return contained control
     */
    public RadioButton getRadioButton() {
        return this.control();
    }
}
