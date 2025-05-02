/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import java.util.Optional;

/**
 * A widget that displays plain inline text.
 */
public final class TextWidget extends InlineWidget implements HasStyledText, HasColor {
    /**
     * Text model binding — handles model and listener for text changes.
     */
    private final ModelBinding<String> text;

    /**
     * Color model binding — handles model and listener for color changes.
     */
    private final ModelBinding<Color> color;

    /**
     * Font face model binding — handles model and listener for font face changes.
     */
    private final ModelBinding<FontFace> fontFace;

    /**
     * Constructs a new {@code TextWidget} with default text and color models.
     *
     * @param client The owning client instance
     * @param parent The parent container (nullable for root)
     */
    TextWidget(final Client client, final Container parent) {
        super(client, parent);
        this.text = new ModelBinding<>(
            new DefaultStringModel(),
            new TextModelListener(this)
        );
        this.color = new ModelBinding<>(
            new DefaultColorModel(),
            new ColorModelListener(this)
        );
        this.fontFace = new ModelBinding<>(
            new DefaultFontFaceModel(),
            new FontFaceModelListener(this)
        );
    }

    /**
     * Constructs a new {@code TextWidget} with initial text.
     *
     * @param client The owning client instance
     * @param parent The parent container (nullable for root)
     * @param text Initial text to display
     */
    public TextWidget(final Client client, final Container parent, final String text) {
        this(client, parent);
        this.text.getModel().setData(text);
    }

    @Override
    public boolean accept(final WidgetVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    String getType() {
        return "text";
    }

    @Override
    void handleEvent(final String type, final Optional<JsonObject> data) {
        // This widget does not handle events
    }

    @Override
    public Model<String> getTextModel() {
        return this.text.getModel();
    }

    @Override
    public void setTextModel(final Model<String> model) {
        this.text.setModel(model);
    }

    @Override
    public Model<Color> getColorModel() {
        return this.color.getModel();
    }

    @Override
    public void setColorModel(final Model<Color> model) {
        this.color.setModel(model);
    }

    @Override
    public Model<FontFace> getFontFaceModel() {
        return this.fontFace.getModel();
    }

    @Override
    public void setFontFaceModel(Model<FontFace> model) {
        this.fontFace.setModel(model);
    }
}
