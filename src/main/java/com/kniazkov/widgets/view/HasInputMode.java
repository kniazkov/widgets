/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.common.InputMode;

/**
 * An entity with a reactive inputmode property.
 */
public interface HasInputMode extends Entity {
    /**
     * @return property model
     */
    default Model<InputMode> getInputModeModel() {
        return this.getModel(State.ANY, Property.INPUT_MODE);
    }
    /**
     * @param model replacement property model
     */
    default void setInputModeModel(final Model<InputMode> model) {
        this.setModel(State.ANY, Property.INPUT_MODE, model);
    }
    /**
     * @return current property value
     */
    default InputMode getInputMode() {
        return this.getInputModeModel().getData();
    }
    /**
     * @param value new property value
     */
    default void setInputMode(final InputMode value) {
        this.getInputModeModel().setData(value);
    }
}
