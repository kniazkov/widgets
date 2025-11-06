/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose text style changes when it gains input focus.
 */
public interface HasFocusStyledText extends HasStyledText {

    /**
     * Returns the model that stores the font face value used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @return the focus font face model
     */
    default Model<FontFace> getFocusFontFaceModel() {
        return this.getModel(State.ACTIVE, Property.FONT_FACE);
    }

    /**
     * Sets the model that stores the font face value used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @param model the new focus font face model to set
     */
    default void setFocusFontFaceModel(final Model<FontFace> model) {
        this.setModel(State.ACTIVE, Property.FONT_FACE, model);
    }

    /**
     * Returns the current focus font face from the associated model.
     *
     * @return the current focus font face
     */
    default FontFace getFocusFontFace() {
        return this.getFocusFontFaceModel().getData();
    }

    /**
     * Updates the focus font face value in the associated model.
     *
     * @param face the new focus font face to set
     */
    default void setFocusFontFace(final FontFace face) {
        this.getFocusFontFaceModel().setData(face);
    }

    /**
     * Returns the model that stores the font size value used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @return the focus font size model
     */
    default Model<FontSize> getFocusFontSizeModel() {
        return this.getModel(State.ACTIVE, Property.FONT_SIZE);
    }

    /**
     * Sets the model that stores the font size value used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @param model the new focus font size model to set
     */
    default void setFocusFontSizeModel(final Model<FontSize> model) {
        this.setModel(State.ACTIVE, Property.FONT_SIZE, model);
    }

    /**
     * Returns the current focus font size from the associated model.
     *
     * @return the current focus font size
     */
    default FontSize getFocusFontSize() {
        return this.getFocusFontSizeModel().getData();
    }

    /**
     * Updates the focus font size value in the associated model.
     *
     * @param size the new focus font size to set
     */
    default void setFocusFontSize(final FontSize size) {
        this.getFocusFontSizeModel().setData(size);
    }

    /**
     * Returns the model that stores the font weight value used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @return the focus font weight model
     */
    default Model<FontWeight> getFocusFontWeightModel() {
        return this.getModel(State.ACTIVE, Property.FONT_WEIGHT);
    }

    /**
     * Sets the model that stores the font weight value used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @param model the new focus font weight model to set
     */
    default void setFocusFontWeightModel(final Model<FontWeight> model) {
        this.setModel(State.ACTIVE, Property.FONT_WEIGHT, model);
    }

    /**
     * Returns the current focus font weight from the associated model.
     *
     * @return the current focus font weight
     */
    default FontWeight getFocusFontWeight() {
        return this.getFocusFontWeightModel().getData();
    }

    /**
     * Updates the focus font weight value in the associated model.
     *
     * @param weight the new focus font weight to set
     */
    default void setFocusFontWeight(final FontWeight weight) {
        this.getFocusFontWeightModel().setData(weight);
    }

    /**
     * Returns the model that stores the italic flag used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @return the focus italic model
     */
    default Model<Boolean> getFocusItalicModel() {
        return this.getModel(State.ACTIVE, Property.ITALIC);
    }

    /**
     * Sets the model that stores the italic flag used when the widget is in the
     * {@link State#ACTIVE} state.
     *
     * @param model the new focus italic model to set
     */
    default void setFocusItalicModel(final Model<Boolean> model) {
        this.setModel(State.ACTIVE, Property.ITALIC, model);
    }

    /**
     * Returns the current focus italic flag from the associated model.
     *
     * @return {@code true} if text is italic in focus state, otherwise {@code false}
     */
    default boolean isFocusItalic() {
        return this.getFocusItalicModel().getData();
    }

    /**
     * Updates the focus italic flag in the associated model.
     *
     * @param italic the new focus italic flag to set
     */
    default void setFocusItalic(final boolean italic) {
        this.getFocusItalicModel().setData(italic);
    }
}
