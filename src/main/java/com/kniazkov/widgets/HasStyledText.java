/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Represents a widget that displays styled text.
 * <p>
 *     In addition to plain text ({@link HasText}), this interface allows accessing and modifying
 *     the font face (i.e., font family) and font weight (i.e., thickness) via models.
 *     This enables two-way data binding and reactive updates.
 * </p>
 */
public interface HasStyledText extends HasText {

    /**
     * Returns the model used to store and manage the font face of this text.
     *
     * @return The model containing the font face
     */
    Model<FontFace> getFontFaceModel();

    /**
     * Sets the model used to store and manage the font face of this text.
     *
     * @param model The new font face model
     */
    void setFontFaceModel(Model<FontFace> model);

    /**
     * Returns the current font face used for rendering the text.
     *
     * @return The current font face
     */
    default FontFace getFontFace() {
        return this.getFontFaceModel().getData();
    }

    /**
     * Sets the font face used for rendering the text.
     *
     * @param fontFace The font face to apply
     */
    default void setFontFace(FontFace fontFace) {
        this.getFontFaceModel().setData(fontFace);
    }

    /**
     * Sets the font face using a raw font family name.
     *
     * @param fontFace The font face name to apply
     */
    default void setFontFace(String fontFace) {
        this.setFontFace(() -> fontFace);
    }

    /**
     * Returns the model used to store and manage the font weight of this text.
     *
     * @return The model containing the font weight
     */
    Model<FontWeight> getFontWeightModel();

    /**
     * Sets the model used to store and manage the font weight of this text.
     *
     * @param model The new font weight model
     */
    void setFontWeightModel(Model<FontWeight> model);

    /**
     * Returns the current font weight used for rendering the text.
     *
     * @return The current font weight
     */
    default FontWeight getFontWeight() {
        return this.getFontWeightModel().getData();
    }

    /**
     * Sets the font weight used for rendering the text.
     *
     * @param fontWeight The font weight to apply
     */
    default void setFontWeight(FontWeight fontWeight) {
        this.getFontWeightModel().setData(fontWeight);
    }
}
