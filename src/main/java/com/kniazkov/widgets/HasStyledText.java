/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Represents a widget that displays styled text.
 * <p>
 *     In addition to plain text ({@link HasText}), this interface allows accessing and modifying
 *     the font face (i.e., font family) via a model. This enables two-way data binding and
 *     reactive updates.
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
}
