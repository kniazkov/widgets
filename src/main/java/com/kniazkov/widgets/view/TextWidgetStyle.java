/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.DefaultBooleanModel;
import com.kniazkov.widgets.model.DefaultColorModel;
import com.kniazkov.widgets.model.DefaultFontFaceModel;
import com.kniazkov.widgets.model.DefaultFontSizeModel;
import com.kniazkov.widgets.model.DefaultFontWeightModel;
import com.kniazkov.widgets.model.ReadOnlyModel;

/**
 * Defines the visual style for text-based widgets.
 */
public class TextWidgetStyle extends Style implements HasStyledText, HasColor {
    /**
     * The default immutable text widget style used as the root prototype.
     */
    public static final TextWidgetStyle DEFAULT = new DefaultTextWidgetStyle();

    /**
     * Creates a new {@code TextWidgetStyle} instance that derives from {@link #DEFAULT}.
     */
    public TextWidgetStyle() {
        this(TextWidgetStyle.DEFAULT);
    }

    /**
     * Creates a new {@code TextWidgetStyle} with the specified prototype.
     *
     * @param prototype the style to inherit properties from
     */
    protected TextWidgetStyle(final TextWidgetStyle prototype) {
        super(prototype);
    }

    @Override
    public TextWidgetStyle derive() {
        return new TextWidgetStyle(this);
    }

    /**
     * Default text widget style.
     */
    private static class DefaultTextWidgetStyle extends TextWidgetStyle {
        /**
         * Constructs the default text widget style without a prototype.
         */
        private DefaultTextWidgetStyle() {
            super(null);
            this.bind(
                Property.FONT_FACE,
                WidgetState.NORMAL,
                ReadOnlyModel.create(FontFace.DEFAULT),
                new FontFaceModelListener(this),
                DefaultFontFaceModel.FACTORY
            );
            this.bind(
                Property.FONT_SIZE,
                WidgetState.NORMAL,
                ReadOnlyModel.create(FontSize.DEFAULT),
                new FontSizeModelListener(this),
                DefaultFontSizeModel.FACTORY
            );
            this.bind(
                Property.FONT_WEIGHT,
                WidgetState.NORMAL,
                ReadOnlyModel.create(FontWeight.NORMAL),
                new FontWeightModelListener(this),
                DefaultFontWeightModel.FACTORY
            );
            this.bind(
                Property.ITALIC,
                WidgetState.NORMAL,
                ReadOnlyModel.create(Boolean.FALSE),
                new ItalicModelListener(this),
                DefaultBooleanModel.FACTORY
            );
            this.bind(
                Property.COLOR,
                WidgetState.NORMAL,
                ReadOnlyModel.create(Color.BLACK),
                new ColorModelListener(this),
                DefaultColorModel.FACTORY
            );
        }
    }
}
