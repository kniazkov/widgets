/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.StringModel;

/**
 * A CheckBox with a caption on its right. Clicking the caption selects the control.
 * Disabled state is shared by the control and caption; the caption becomes gray.
 */
public final class CheckBoxWithText extends LabeledChoice<CheckBox> {
    /**
     * Creates an unchecked control with an empty caption.
     */
    public CheckBoxWithText() {
        this("");
    }

    /**
     * Creates an unchecked control with the supplied caption.
     * @param text initial caption
     */
    public CheckBoxWithText(final String text) {
        this(new StringModel(text), new BooleanModel(false));
    }

    /**
     * Binds existing caption and selection models directly.
     * @param text caption model
     * @param checked selection model
     */
    public CheckBoxWithText(final Model<String> text, final Model<Boolean> checked) {
        super(new CheckBox(), text, checked, false);
    }

    /**
     * Returns the contained CheckBox for customization.
     * @return contained control
     */
    public CheckBox getCheckBox() {
        return this.control();
    }
}
