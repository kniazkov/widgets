/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} whose text style changes when the pointer hovers over it.
 */
public interface HasHoverStyledText extends HasStyledText {

    /**
     * Returns the model that stores the font face value used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @return the hover font face model
     */
    default Model<FontFace> getHoverFontFaceModel() {
        return this.getModel(State.HOVERED, Property.FONT_FACE);
    }

    /**
     * Sets the model that stores the font face value used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @param model the new hover font face model to set
     */
    default void setHoverFontFaceModel(final Model<FontFace> model) {
        this.setModel(State.HOVERED, Property.FONT_FACE, model);
    }

    /**
     * Returns the current hover font face from the associated model.
     *
     * @return the current hover font face
     */
    default FontFace getHoverFontFace() {
        return this.getHoverFontFaceModel().getData();
    }

    /**
     * Updates the hover font face value in the associated model.
     *
     * @param face the new hover font face to set
     */
    default void setHoverFontFace(final FontFace face) {
        this.getHoverFontFaceModel().setData(face);
    }

    /**
     * Returns the model that stores the font size value used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @return the hover font size model
     */
    default Model<FontSize> getHoverFontSizeModel() {
        return this.getModel(State.HOVERED, Property.FONT_SIZE);
    }

    /**
     * Sets the model that stores the font size value used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @param model the new hover font size model to set
     */
    default void setHoverFontSizeModel(final Model<FontSize> model) {
        this.setModel(State.HOVERED, Property.FONT_SIZE, model);
    }

    /**
     * Returns the current hover font size from the associated model.
     *
     * @return the current hover font size
     */
    default FontSize getHoverFontSize() {
        return this.getHoverFontSizeModel().getData();
    }

    /**
     * Updates the hover font size value in the associated model.
     *
     * @param size the new hover font size to set
     */
    default void setHoverFontSize(final FontSize size) {
        this.getHoverFontSizeModel().setData(size);
    }

    /**
     * Returns the model that stores the font weight value used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @return the hover font weight model
     */
    default Model<FontWeight> getHoverFontWeightModel() {
        return this.getModel(State.HOVERED, Property.FONT_WEIGHT);
    }

    /**
     * Sets the model that stores the font weight value used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @param model the new hover font weight model to set
     */
    default void setHoverFontWeightModel(final Model<FontWeight> model) {
        this.setModel(State.HOVERED, Property.FONT_WEIGHT, model);
    }

    /**
     * Returns the current hover font weight from the associated model.
     *
     * @return the current hover font weight
     */
    default FontWeight getHoverFontWeight() {
        return this.getHoverFontWeightModel().getData();
    }

    /**
     * Updates the hover font weight value in the associated model.
     *
     * @param weight the new hover font weight to set
     */
    default void setHoverFontWeight(final FontWeight weight) {
        this.getHoverFontWeightModel().setData(weight);
    }

    /**
     * Returns the model that stores the italic flag used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @return the hover italic model
     */
    default Model<Boolean> getHoverItalicModel() {
        return this.getModel(State.HOVERED, Property.ITALIC);
    }

    /**
     * Sets the model that stores the italic flag used when the widget is in the
     * {@link State#HOVERED} state.
     *
     * @param model the new hover italic model to set
     */
    default void setHoverItalicModel(final Model<Boolean> model) {
        this.setModel(State.HOVERED, Property.ITALIC, model);
    }

    /**
     * Returns the current hover italic flag from the associated model.
     *
     * @return {@code true} if text is italic in hover state, otherwise {@code false}
     */
    default boolean isHoverItalic() {
        return this.getHoverItalicModel().getData();
    }

    /**
     * Updates the hover italic flag in the associated model.
     *
     * @param italic the new hover italic flag to set
     */
    default void setHoverItalic(final boolean italic) {
        this.getHoverItalicModel().setData(italic);
    }
}
