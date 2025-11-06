/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose text style changes when it becomes disabled.
 */
public interface HasDisabledStyledText extends HasStyledText {

    /**
     * Returns the model that stores the font face value used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @return the disabled font face model
     */
    default Model<FontFace> getDisabledFontFaceModel() {
        return this.getModel(State.DISABLED, Property.FONT_FACE);
    }

    /**
     * Sets the model that stores the font face value used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @param model the new disabled font face model to set
     */
    default void setDisabledFontFaceModel(final Model<FontFace> model) {
        this.setModel(State.DISABLED, Property.FONT_FACE, model);
    }

    /**
     * Returns the current disabled font face from the associated model.
     *
     * @return the current disabled font face
     */
    default FontFace getDisabledFontFace() {
        return this.getDisabledFontFaceModel().getData();
    }

    /**
     * Updates the disabled font face value in the associated model.
     *
     * @param face the new disabled font face to set
     */
    default void setDisabledFontFace(final FontFace face) {
        this.getDisabledFontFaceModel().setData(face);
    }

    /**
     * Returns the model that stores the font size value used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @return the disabled font size model
     */
    default Model<FontSize> getDisabledFontSizeModel() {
        return this.getModel(State.DISABLED, Property.FONT_SIZE);
    }

    /**
     * Sets the model that stores the font size value used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @param model the new disabled font size model to set
     */
    default void setDisabledFontSizeModel(final Model<FontSize> model) {
        this.setModel(State.DISABLED, Property.FONT_SIZE, model);
    }

    /**
     * Returns the current disabled font size from the associated model.
     *
     * @return the current disabled font size
     */
    default FontSize getDisabledFontSize() {
        return this.getDisabledFontSizeModel().getData();
    }

    /**
     * Updates the disabled font size value in the associated model.
     *
     * @param size the new disabled font size to set
     */
    default void setDisabledFontSize(final FontSize size) {
        this.getDisabledFontSizeModel().setData(size);
    }

    /**
     * Returns the model that stores the font weight value used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @return the disabled font weight model
     */
    default Model<FontWeight> getDisabledFontWeightModel() {
        return this.getModel(State.DISABLED, Property.FONT_WEIGHT);
    }

    /**
     * Sets the model that stores the font weight value used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @param model the new disabled font weight model to set
     */
    default void setDisabledFontWeightModel(final Model<FontWeight> model) {
        this.setModel(State.DISABLED, Property.FONT_WEIGHT, model);
    }

    /**
     * Returns the current disabled font weight from the associated model.
     *
     * @return the current disabled font weight
     */
    default FontWeight getDisabledFontWeight() {
        return this.getDisabledFontWeightModel().getData();
    }

    /**
     * Updates the disabled font weight value in the associated model.
     *
     * @param weight the new disabled font weight to set
     */
    default void setDisabledFontWeight(final FontWeight weight) {
        this.getDisabledFontWeightModel().setData(weight);
    }

    /**
     * Returns the model that stores the italic flag used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @return the disabled italic model
     */
    default Model<Boolean> getDisabledItalicModel() {
        return this.getModel(State.DISABLED, Property.ITALIC);
    }

    /**
     * Sets the model that stores the italic flag used when the widget is in the
     * {@link State#DISABLED} state.
     *
     * @param model the new disabled italic model to set
     */
    default void setDisabledItalicModel(final Model<Boolean> model) {
        this.setModel(State.DISABLED, Property.ITALIC, model);
    }

    /**
     * Returns the current disabled italic flag from the associated model.
     *
     * @return {@code true} if text is italic in disabled state, otherwise {@code false}
     */
    default boolean isDisabledItalic() {
        return this.getDisabledItalicModel().getData();
    }

    /**
     * Updates the disabled italic flag in the associated model.
     *
     * @param italic the new disabled italic flag to set
     */
    default void setDisabledItalic(final boolean italic) {
        this.getDisabledItalicModel().setData(italic);
    }
}
