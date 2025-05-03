/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Style definition for {@link TextWidget}, specifying how text should be rendered.
 * <p>
 *     Currently supports font face, font weight, and italic customization via models.
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
     * Model for italic rendering.
     */
    private final Model<Boolean> italic;

    /**
     * Constructs a new text style using default models.
     */
    TextWidgetStyle() {
        this.fontFace = new DefaultFontFaceModel();
        this.fontWeight = new DefaultFontWeightModel();
        this.italic = new DefaultBooleanModel();
    }

    /**
     * Internal constructor for forking.
     *
     * @param fontFace Forked font face model
     * @param fontWeight Forked font weight model
     * @param italic Forked italic model
     */
    private TextWidgetStyle(Model<FontFace> fontFace, Model<FontWeight> fontWeight,
            Model<Boolean> italic) {
        this.fontFace = fontFace;
        this.fontWeight = fontWeight;
        this.italic = italic;
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

    /**
     * Returns the model used to store and manage italic rendering of the text.
     *
     * @return The model containing the italic state
     */
    public Model<Boolean> getItalicModel() {
        return this.italic;
    }

    /**
     * Returns whether the text is currently rendered in italic style.
     *
     * @return {@code true} if italic, {@code false} otherwise
     */
    public boolean isItalic() {
        return this.italic.getData();
    }

    /**
     * Sets whether the text should be rendered in italic style.
     *
     * @param value {@code true} to enable italic, {@code false} to disable
     */
    public void setItalic(boolean value) {
        this.italic.setData(value);
    }

    @Override
    public void apply(TextWidget widget) {
        widget.setFontFaceModel(this.fontFace.fork());
        widget.setFontWeightModel(this.fontWeight.fork());
        widget.setItalicModel(this.italic.fork());
    }

    @Override
    public TextWidgetStyle fork() {
        return new TextWidgetStyle(
            this.fontFace.fork(),
            this.fontWeight.fork(),
            this.italic.fork()
        );
    }
}
