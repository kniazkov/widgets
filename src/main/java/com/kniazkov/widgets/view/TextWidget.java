/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.ColorModel;
import com.kniazkov.widgets.model.FontFaceModel;
import com.kniazkov.widgets.model.FontSizeModel;
import com.kniazkov.widgets.model.FontWeightModel;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.model.Binding;
import java.util.Optional;

/**
 * A simple text widget.
 */
public class TextWidget extends InlineWidget implements HasStyledText, HasColor {
    /**
     * Binding between the color model and this widget.
     */
    final Binding<Color> color;

    /**
     * Binding between the font face model and this widget.
     */
    final Binding<FontFace> fontFace;

    /**
     * Binding between the font size model and this widget.
     */
    final Binding<FontSize> fontSize;

    /**
     * Binding between the font weight model and this widget.
     */
    final Binding<FontWeight> fontWeight;

    /**
     * Binding between the italic model and this widget.
     */
    final Binding<Boolean> italic;

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
        this.bindModel(
            State.NORMAL,
            Property.TEXT,
            new StringModel(text),
            new TextModelListener(this)
        );
        this.color = new Binding<>(
            new ColorModel(),
            new ColorModelListener(this)
        );
        this.fontFace = new Binding<>(
            new FontFaceModel(),
            new FontFaceModelListener(this)
        );
        this.fontSize = new Binding<>(
            new FontSizeModel(),
            new FontSizeModelListener(this))
        ;
        this.fontWeight = new Binding<>(
            new FontWeightModel(),
            new FontWeightModelListener(this)
        );
        this.italic = new Binding<>(
            new BooleanModel(),
            new ItalicModelListener(this)
        );
    }

    @Override
    public Binding<Color> getColorModelBinding() {
        return this.color;
    }

    @Override
    public Binding<FontFace> getFontFaceModelBinding() {
        return this.fontFace;
    }

    @Override
    public Binding<FontSize> getFontSizeModelBinding() {
        return this.fontSize;
    }

    @Override
    public Binding<FontWeight> getFontWeightModelBinding() {
        return this.fontWeight;
    }

    @Override
    public Binding<Boolean> getItalicModelBinding() {
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
