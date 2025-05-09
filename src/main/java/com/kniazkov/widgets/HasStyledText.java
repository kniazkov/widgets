/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Represents a widget that displays styled text.
 * <p>
 *     In addition to plain text ({@link HasText}), this interface allows accessing and modifying
 *     the font face (i.e., font family), font weight (i.e., thickness), and italic style
 *     via models. This enables two-way data binding and reactive updates.
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
     * Returns the model used to store and manage the font size of this text.
     *
     * @return The model containing the font size
     */
    Model<FontSize> getFontSizeModel();

    /**
     * Sets the model used to store and manage the font size of this text.
     *
     * @param model The new font size model
     */
    void setFontSizeModel(Model<FontSize> model);

    /**
     * Returns the current font size used for rendering the text.
     *
     * @return The current font size
     */
    default FontSize getFontSize() {
        return this.getFontSizeModel().getData();
    }

    /**
     * Sets the font size used for rendering the text.
     *
     * @param size The font size to apply
     */
    default void setFontSize(FontSize size) {
        this.getFontSizeModel().setData(size);
    }

    /**
     * Sets the font size using a CSS-style string (e.g., "12pt", "14px").
     *
     * @param size The string representing font size
     */
    default void setFontSize(String size) {
        this.setFontSize(new FontSize(size));
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

    /**
     * Returns the model used to store and manage italic rendering of the text.
     *
     * @return The model containing the italic state
     */
    Model<Boolean> getItalicModel();

    /**
     * Sets the model used to store and manage italic rendering of the text.
     *
     * @param model The new italic model
     */
    void setItalicModel(Model<Boolean> model);

    /**
     * Returns whether the text is currently rendered in italic style.
     *
     * @return {@code true} if italic, {@code false} otherwise
     */
    default boolean isItalic() {
        return this.getItalicModel().getData();
    }

    /**
     * Sets whether the text should be rendered in italic style.
     *
     * @param italic {@code true} to enable italic, {@code false} to disable
     */
    default void setItalic(boolean italic) {
        this.getItalicModel().setData(italic);
    }
}
