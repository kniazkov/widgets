/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Style definition for {@link Button}, specifying how button should be rendered.
 * <p>
 *     Currently supports background color customization via models.
 * </p>
 */
public final class ButtonStyle implements Style<Button> {
    /**
     * Model for the background color.
     */
    private final Model<Color> bgColor;

    /**
     * Constructs a new text style using default models.
     */
    ButtonStyle() {
        this.bgColor = new DefaultColorModel();

        this.bgColor.setData(Color.WHITE);
    }

    /**
     * Internal constructor for forking.
     *
     * @param bgColor Forked background color
     */
    private ButtonStyle(final Model<Color> bgColor) {
        this.bgColor = bgColor;
    }

    /**
     * Returns the background model used in this style.
     *
     * @return Background color model
     */
    public Model<Color> getBackgroundColorModel() {
        return this.bgColor;
    }

    /**
     * Returns the current background color value.
     *
     * @return Background color
     */
    public Color getBackgroundColor() {
        return this.bgColor.getData();
    }

    /**
     * Updates the background color value.
     *
     * @param value New background color
     */
    public void setFontFace(final Color value) {
        this.bgColor.setData(value);
    }

    @Override
    public void apply(Button widget) {
        widget.setBackgroundColorModel(this.bgColor.fork());
    }

    @Override
    public ButtonStyle fork() {
        return new ButtonStyle(
            this.bgColor.fork()
        );
    }
}
