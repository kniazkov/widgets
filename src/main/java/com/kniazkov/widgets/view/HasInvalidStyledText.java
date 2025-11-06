/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose text style changes when its validation state is invalid.
 */
public interface HasInvalidStyledText extends HasStyledText {

    /**
     * Returns the model that stores the font face value used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @return the invalid font face model
     */
    default Model<FontFace> getInvalidFontFaceModel() {
        return this.getModel(State.INVALID, Property.FONT_FACE);
    }

    /**
     * Sets the model that stores the font face value used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @param model the new invalid font face model to set
     */
    default void setInvalidFontFaceModel(final Model<FontFace> model) {
        this.setModel(State.INVALID, Property.FONT_FACE, model);
    }

    /**
     * Returns the current invalid font face from the associated model.
     *
     * @return the current invalid font face
     */
    default FontFace getInvalidFontFace() {
        return this.getInvalidFontFaceModel().getData();
    }

    /**
     * Updates the invalid font face value in the associated model.
     *
     * @param face the new invalid font face to set
     */
    default void setInvalidFontFace(final FontFace face) {
        this.getInvalidFontFaceModel().setData(face);
    }

    /**
     * Returns the model that stores the font size value used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @return the invalid font size model
     */
    default Model<FontSize> getInvalidFontSizeModel() {
        return this.getModel(State.INVALID, Property.FONT_SIZE);
    }

    /**
     * Sets the model that stores the font size value used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @param model the new invalid font size model to set
     */
    default void setInvalidFontSizeModel(final Model<FontSize> model) {
        this.setModel(State.INVALID, Property.FONT_SIZE, model);
    }

    /**
     * Returns the current invalid font size from the associated model.
     *
     * @return the current invalid font size
     */
    default FontSize getInvalidFontSize() {
        return this.getInvalidFontSizeModel().getData();
    }

    /**
     * Updates the invalid font size value in the associated model.
     *
     * @param size the new invalid font size to set
     */
    default void setInvalidFontSize(final FontSize size) {
        this.getInvalidFontSizeModel().setData(size);
    }

    /**
     * Returns the model that stores the font weight value used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @return the invalid font weight model
     */
    default Model<FontWeight> getInvalidFontWeightModel() {
        return this.getModel(State.INVALID, Property.FONT_WEIGHT);
    }

    /**
     * Sets the model that stores the font weight value used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @param model the new invalid font weight model to set
     */
    default void setInvalidFontWeightModel(final Model<FontWeight> model) {
        this.setModel(State.INVALID, Property.FONT_WEIGHT, model);
    }

    /**
     * Returns the current invalid font weight from the associated model.
     *
     * @return the current invalid font weight
     */
    default FontWeight getInvalidFontWeight() {
        return this.getInvalidFontWeightModel().getData();
    }

    /**
     * Updates the invalid font weight value in the associated model.
     *
     * @param weight the new invalid font weight to set
     */
    default void setInvalidFontWeight(final FontWeight weight) {
        this.getInvalidFontWeightModel().setData(weight);
    }

    /**
     * Returns the model that stores the italic flag used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @return the invalid italic model
     */
    default Model<Boolean> getInvalidItalicModel() {
        return this.getModel(State.INVALID, Property.ITALIC);
    }

    /**
     * Sets the model that stores the italic flag used when the widget is in the
     * {@link State#INVALID} state.
     *
     * @param model the new invalid italic model to set
     */
    default void setInvalidItalicModel(final Model<Boolean> model) {
        this.setModel(State.INVALID, Property.ITALIC, model);
    }

    /**
     * Returns the current invalid italic flag from the associated model.
     *
     * @return {@code true} if text is italic in invalid state, otherwise {@code false}
     */
    default boolean isInvalidItalic() {
        return this.getInvalidItalicModel().getData();
    }

    /**
     * Updates the invalid italic flag in the associated model.
     *
     * @param italic the new invalid italic flag to set
     */
    default void setInvalidItalic(final boolean italic) {
        this.getInvalidItalicModel().setData(italic);
    }
}
