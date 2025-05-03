/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Style definition for {@link TextWidget}, specifying how text should be rendered.
 * <p>
 *     Currently supports font face, font size, font weight, and italic customization via models.
 * </p>
 */
public final class TextWidgetStyle implements Style<TextWidget> {
    /**
     * Model for the font face.
     */
    private final Model<FontFace> fontFace;

    /**
     * Model for font size.
     */
    private final Model<FontSize> fontSize;

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
        this.fontSize = new DefaultFontSizeModel();
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
    private TextWidgetStyle(final Model<FontFace> fontFace, final Model<FontSize> fontSize,
            final Model<FontWeight> fontWeight, final Model<Boolean> italic) {
        this.fontFace = fontFace;
        this.fontSize = fontSize;
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
    public void setFontFace(final FontFace value) {
        this.fontFace.setData(value);
    }

    /**
     * Sets the font face using a raw font family name.
     *
     * @param fontFace The font face name to apply
     */
    public void setFontFace(final String fontFace) {
        this.setFontFace(() -> fontFace);
    }

    /**
     * Returns the font size model used in this style.
     *
     * @return Font size model
     */
    public Model<FontSize> getFontSizeModel() {
        return this.fontSize;
    }

    /**
     * Updates the font size using a CSS-style string (e.g., "14px", "10pt").
     *
     * @param value Font size string
     */
    public void setFontSize(final String value) {
        this.setFontSize(new FontSize(value));
    }

    /**
     * Returns the current font size value.
     *
     * @return Font size
     */
    public FontSize getFontSize() {
        return this.fontSize.getData();
    }

    /**
     * Updates the font size value.
     *
     * @param value New font size
     */
    public void setFontSize(FontSize value) {
        this.fontSize.setData(value);
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
    public void setFontWeight(final FontWeight value) {
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
    public void setItalic(final boolean value) {
        this.italic.setData(value);
    }

    @Override
    public void apply(TextWidget widget) {
        widget.setFontFaceModel(this.fontFace.fork());
        widget.setFontSizeModel(this.fontSize.fork());
        widget.setFontWeightModel(this.fontWeight.fork());
        widget.setItalicModel(this.italic.fork());
    }

    @Override
    public TextWidgetStyle fork() {
        return new TextWidgetStyle(
            this.fontFace.fork(),
            this.fontSize.fork(),
            this.fontWeight.fork(),
            this.italic.fork()
        );
    }
}
