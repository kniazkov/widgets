/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.DefaultBooleanModel;
import com.kniazkov.widgets.model.DefaultColorModel;
import com.kniazkov.widgets.model.DefaultFontFaceModel;
import com.kniazkov.widgets.model.DefaultFontSizeModel;
import com.kniazkov.widgets.model.DefaultFontWeightModel;
import com.kniazkov.widgets.model.DefaultStringModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;
import java.util.Optional;

/**
 * A simple text widget.
 */
public class TextWidget extends InlineWidget implements HasStyledText, HasColor {
    /**
     * Binding between the color model and this widget.
     */
    final ModelBinding<Color> color;

    /**
     * Binding between the font face model and this widget.
     */
    final ModelBinding<FontFace> fontFace;

    /**
     * Binding between the font size model and this widget.
     */
    final ModelBinding<FontSize> fontSize;

    /**
     * Binding between the font weight model and this widget.
     */
    final ModelBinding<FontWeight> fontWeight;

    /**
     * Binding between the italic model and this widget.
     */
    final ModelBinding<Boolean> italic;

    /**
     * Creates a new text widget with empty text.
     */
    public TextWidget() {
        this("");
    }

    /**
     * Creates a new text widget with the given initial text.
     *
     * @param text the initial text
     */
    public TextWidget(final String text) {
        this.addBinding(
            Property.TEXT,
            new ModelBinding<>(
                new DefaultStringModel(text),
                new TextModelListener(this),
                DefaultStringModel.FACTORY
            )
        );
        this.color = new ModelBinding<>(
            new DefaultColorModel(),
            new ColorModelListener(this),
            DefaultColorModel.FACTORY
        );
        this.fontFace = new ModelBinding<>(
            new DefaultFontFaceModel(),
            new FontFaceModelListener(this),
            DefaultFontFaceModel.FACTORY
        );
        this.fontSize = new ModelBinding<>(
            new DefaultFontSizeModel(),
            new FontSizeModelListener(this),
            DefaultFontSizeModel.FACTORY
        );
        this.fontWeight = new ModelBinding<>(
            new DefaultFontWeightModel(),
            new FontWeightModelListener(this),
            DefaultFontWeightModel.FACTORY
        );
        this.italic = new ModelBinding<>(
            new DefaultBooleanModel(),
            new ItalicModelListener(this),
            DefaultBooleanModel.FACTORY
        );
    }

    @Override
    public ModelBinding<Color> getColorModelBinding() {
        return this.color;
    }

    @Override
    public ModelBinding<FontFace> getFontFaceModelBinding() {
        return this.fontFace;
    }

    @Override
    public ModelBinding<FontSize> getFontSizeModelBinding() {
        return this.fontSize;
    }

    @Override
    public ModelBinding<FontWeight> getFontWeightModelBinding() {
        return this.fontWeight;
    }

    @Override
    public ModelBinding<Boolean> getItalicModelBinding() {
        return this.italic;
    }

    @Override
    public String getType() {
        return "text";
    }

    @Override
    public void handleEvent(String type, Optional<JsonObject> data) {
        // no events to process
    }
}
