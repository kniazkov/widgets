/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that displays stylable text.
 * Extends {@link HasText} to support additional text style attributes:
 * <ul>
 *   <li><b>Font face</b> — the typeface used to render the text ({@link FontFace})</li>
 *   <li><b>Font size</b> — the text size in CSS units ({@link FontSize})</li>
 *   <li><b>Font weight</b> — the text weight (e.g., 400, 700) ({@link FontWeight})</li>
 *   <li><b>Italic</b> — whether the text is rendered in italics ({@code boolean})</li>
 * </ul>
 */
public interface HasStyledText extends HasText {

    /**
     * Returns the model that stores the font face for this view.
     *
     * @return the font face model
     */
    default Model<FontFace> getFontFaceModel() {
        return this.getModel(State.NORMAL, Property.FONT_FACE);
    }

    /**
     * Sets a new font face model for this view.
     *
     * @param model the font face model to set
     */
    default void setFontFaceModel(final Model<FontFace> model) {
        this.setModel(State.NORMAL, Property.FONT_FACE, model);
    }

    /**
     * Returns the current font face from the current model.
     *
     * @return the current font face
     */
    default FontFace getFontFace() {
        return this.getFontFaceModel().getData();
    }

    /**
     * Updates the font face in the model.
     *
     * @param face the new font face
     */
    default void setFontFace(final FontFace face) {
        this.getFontFaceModel().setData(face);
    }

    /**
     * Returns the model that stores the font size for this view.
     *
     * @return the font size model
     */
    default Model<FontSize> getFontSizeModel() {
        return this.getModel(State.NORMAL, Property.FONT_SIZE);
    }

    /**
     * Sets a new font size model for this view.
     *
     * @param model the font size model to set
     */
    default void setFontSizeModel(final Model<FontSize> model) {
        this.setModel(State.NORMAL, Property.FONT_SIZE, model);
    }

    /**
     * Returns the current font size from the current model.
     *
     * @return the current font size
     */
    default FontSize getFontSize() {
        return this.getFontSizeModel().getData();
    }

    /**
     * Updates the font size in the model.
     *
     * @param size the new font size
     */
    default void setFontSize(final FontSize size) {
        this.getFontSizeModel().setData(size);
    }

    /**
     * Returns the model that stores the font weight for this view.
     *
     * @return the font weight model
     */
    default Model<FontWeight> getFontWeightModel() {
        return this.getModel(State.NORMAL, Property.FONT_WEIGHT);
    }

    /**
     * Sets a new font weight model for this view.
     *
     * @param model the font weight model to set
     */
    default void setFontWeightModel(final Model<FontWeight> model) {
        this.setModel(State.NORMAL, Property.FONT_WEIGHT, model);
    }

    /**
     * Returns the current font weight from the current model.
     *
     * @return the current font weight
     */
    default FontWeight getFontWeight() {
        return this.getFontWeightModel().getData();
    }

    /**
     * Updates the font weight in the model.
     *
     * @param weight the new font weight
     */
    default void setFontWeight(final FontWeight weight) {
        this.getFontWeightModel().setData(weight);
    }

    /**
     * Returns the model that stores the italic flag for this view.
     *
     * @return the italic model
     */
    default Model<Boolean> getItalicModel() {
        return this.getModel(State.NORMAL, Property.ITALIC);
    }

    /**
     * Sets a new italic model for this view.
     *
     * @param model the italic model to set
     */
    default void setItalicModel(final Model<Boolean> model) {
        this.setModel(State.NORMAL, Property.ITALIC, model);
    }

    /**
     * Returns whether the text is rendered in italics, according to the current model.
     *
     * @return {@code true} if italic style is enabled, otherwise {@code false}
     */
    default boolean isItalic() {
        return this.getItalicModel().getData();
    }

    /**
     * Updates the italic flag in the model.
     *
     * @param italic {@code true} to render text in italics, {@code false} otherwise
     */
    default void setItalic(final boolean italic) {
        this.getItalicModel().setData(italic);
    }
}
