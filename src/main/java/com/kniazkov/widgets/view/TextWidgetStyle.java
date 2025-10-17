/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.DefaultBooleanModel;
import com.kniazkov.widgets.model.DefaultColorModel;
import com.kniazkov.widgets.model.DefaultFontFaceModel;
import com.kniazkov.widgets.model.DefaultFontSizeModel;
import com.kniazkov.widgets.model.DefaultFontWeightModel;

public class TextWidgetStyle extends Style implements HasStyledText, HasColor {
    public static final TextWidgetStyle DEFAULT = new DefaultTextWidgetStyle();

    public TextWidgetStyle() {
        super(TextWidgetStyle.DEFAULT);
    }

    protected TextWidgetStyle(final Style prototype) {
        super(prototype);
    }

    @Override
    public TextWidgetStyle derive() {
        return new TextWidgetStyle(this);
    }

    private static class DefaultTextWidgetStyle extends TextWidgetStyle {
        protected DefaultTextWidgetStyle() {
            super(null);
            this.bind(
                Property.FONT_FACE,
                WidgetState.NORMAL,
                new DefaultFontFaceModel(),
                new FontFaceModelListener(this),
                DefaultFontFaceModel.FACTORY
            );
            this.bind(
                Property.FONT_SIZE,
                WidgetState.NORMAL,
                new DefaultFontSizeModel(),
                new FontSizeModelListener(this),
                DefaultFontSizeModel.FACTORY
            );
            this.bind(
                Property.FONT_WEIGHT,
                WidgetState.NORMAL,
                new DefaultFontWeightModel(),
                new FontWeightModelListener(this),
                DefaultFontWeightModel.FACTORY
            );
            this.bind(
                Property.ITALIC,
                WidgetState.NORMAL,
                new DefaultBooleanModel(false),
                new ItalicModelListener(this),
                DefaultBooleanModel.FACTORY
            );
            this.bind(
                Property.COLOR,
                WidgetState.NORMAL,
                new DefaultColorModel(Color.BLACK),
                new ColorModelListener(this),
                DefaultColorModel.FACTORY
            );
        }
    }
}
