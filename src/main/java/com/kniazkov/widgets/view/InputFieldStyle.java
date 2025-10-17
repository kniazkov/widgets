/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.InlineWidgetSize;
import com.kniazkov.widgets.model.DefaultBooleanModel;
import com.kniazkov.widgets.model.DefaultColorModel;
import com.kniazkov.widgets.model.DefaultFontFaceModel;
import com.kniazkov.widgets.model.DefaultFontSizeModel;
import com.kniazkov.widgets.model.DefaultFontWeightModel;
import com.kniazkov.widgets.model.DefaultInlineWidgetSizeModel;
import com.kniazkov.widgets.model.ReadOnlyModel;

/**
 * Defines the visual style for input fields.
 */
public class InputFieldStyle extends Style implements HasStyledText, HasColor,
        HasBgColor, HasWidth<InlineWidgetSize>, HasHeight<InlineWidgetSize> {
    /**
     * The default immutable input field style used as the root prototype.
     */
    public static final InputFieldStyle DEFAULT = new DefaultTextWidgetStyle();

    /**
     * Creates a new {@code InputFieldStyle} instance that derives from {@link #DEFAULT}.
     */
    public InputFieldStyle() {
        this(InputFieldStyle.DEFAULT);
    }

    /**
     * Creates a new {@code InputFieldStyle} with the specified prototype.
     *
     * @param prototype the style to inherit properties from
     */
    protected InputFieldStyle(final InputFieldStyle prototype) {
        super(prototype);
    }

    @Override
    public InputFieldStyle derive() {
        return new InputFieldStyle(this);
    }

    /**
     * Default input field style.
     */
    private static class DefaultTextWidgetStyle extends InputFieldStyle {
        /**
         * Constructs the default input field style without a prototype.
         */
        protected DefaultTextWidgetStyle() {
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
            this.bind(
                Property.BG_COLOR,
                WidgetState.NORMAL,
                ReadOnlyModel.create(Color.WHITE),
                new BgColorModelListener(this),
                DefaultColorModel.FACTORY
            );
            this.bind(
                Property.WIDTH,
                ReadOnlyModel.create(InlineWidgetSize.UNDEFINED),
                new WidthModelListener<>(this),
                DefaultInlineWidgetSizeModel.FACTORY
            );
            this.bind(
                Property.HEIGHT,
                ReadOnlyModel.create(InlineWidgetSize.UNDEFINED),
                new HeightModelListener<>(this),
                DefaultInlineWidgetSizeModel.FACTORY
            );
        }
    }
}
