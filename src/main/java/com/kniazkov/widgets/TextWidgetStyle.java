/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Style definition for {@link TextWidget}, specifying how text should be rendered.
 * <p>
 *     Currently supports font face and font weight customization via models.
 * </p>
 */
public final class TextWidgetStyle implements Style<TextWidget> {
    /**
     * Model for the font face.
     */
    private final Model<FontFace> fontFace;

    /**
     * Model for the font weight.
     */
    private final Model<FontWeight> fontWeight;

    /**
     * Constructs a new text style using default models.
     */
    TextWidgetStyle() {
        this.fontFace = new DefaultFontFaceModel();
        this.fontWeight = new DefaultFontWeightModel();
    }

    /**
     * Internal constructor for forking.
     *
     * @param fontFace Forked font face model
     * @param fontWeight Forked font weight model
     */
    private TextWidgetStyle(Model<FontFace> fontFace, Model<FontWeight> fontWeight) {
        this.fontFace = fontFace;
        this.fontWeight = fontWeight;
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

    /**
     * Returns the font weight model used in this style.
     *
     * @return Font weight model
     */
    public Model<FontWeight> getFontWeightModel() {
        return this.fontWeight;
    }

    /**
     * Returns the current font weigth value.
     *
     * @return Font weight
     */
    public FontWeight getFontWeight() {
        return this.fontWeight.getData();
    }

    /**
     * Updates the font weight value.
     *
     * @param value New font weight
     */
    public void setFontWeight(FontWeight value) {
        this.fontWeight.setData(value);
    }

    @Override
    public void apply(TextWidget widget) {
        widget.setFontFaceModel(this.fontFace.fork());
        widget.setFontWeightModel(this.fontWeight.fork());
    }

    @Override
    public TextWidgetStyle fork() {
        return new TextWidgetStyle(
            this.fontFace.fork(),
            this.fontWeight.fork()
        );
    }
}
