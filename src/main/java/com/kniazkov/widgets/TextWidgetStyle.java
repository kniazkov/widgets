/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Style definition for {@link TextWidget}, specifying how text should be rendered.
 * <p>
 *     Currently supports font face customization via a model.
 * </p>
 */
public final class TextWidgetStyle implements Style<TextWidget> {
    /**
     * Model for the font face.
     */
    private final Model<FontFace> fontFace;

    /**
     * Constructs a new text style using default models.
     */
    TextWidgetStyle() {
        this.fontFace = new DefaultFontFaceModel();
    }

    /**
     * Internal constructor for forking.
     *
     * @param fontFace Forked font face model
     */
    private TextWidgetStyle(Model<FontFace> fontFace) {
        this.fontFace = fontFace;
    }

    /**
     * Returns the font face model used in this style.
     *
     * @return Font face model
     */
    public Model<FontFace> getFontFaceModel() {
        return this.fontFace;
    }

    /**
     * Returns the current font face value.
     *
     * @return Font face
     */
    public FontFace getFontFace() {
        return this.fontFace.getData();
    }

    /**
     * Updates the font face value.
     *
     * @param value New font face
     */
    public void setFontFace(FontFace value) {
        this.fontFace.setData(value);
    }

    /**
     * Sets the font face using a raw font family name.
     *
     * @param fontFace The font face name to apply
     */
    public void setFontFace(String fontFace) {
        this.setFontFace(() -> fontFace);
    }

    @Override
    public void apply(TextWidget widget) {
        widget.setFontFaceModel(this.fontFace.fork());
    }

    @Override
    public TextWidgetStyle fork() {
        return new TextWidgetStyle(this.fontFace.fork());
    }
}
