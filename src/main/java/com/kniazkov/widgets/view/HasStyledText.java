/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.ModelBinding;
import com.kniazkov.widgets.protocol.SetFontFace;
import com.kniazkov.widgets.protocol.SetFontSize;
import com.kniazkov.widgets.protocol.SetFontWeight;
import com.kniazkov.widgets.protocol.SetItalic;

/**
 * A {@link View} that displays stylable text.
 * Extends {@link HasText} to support additional text style attributes:
 * <ul>
 *   <li><b>Font face</b> — the typeface used to render the text ({@link FontFace})</li>
 *   <li><b>Font size</b> — the text size in CSS units ({@link FontSize})</li>
 *   <li><b>Font weight</b> — the text weight (e.g., 400, 700) ({@link FontWeight})</li>
 *   <li><b>Italic</b> — whether the text is rendered in italics ({@code boolean})</li>
 * </ul>
 */
public interface HasStyledText extends HasText {
    /**
     * Returns a state-dependent binding for the specified property and state.
     *
     * @param property property key
     * @param state widget state
     * @param <T> binding data type
     * @return the model binding
     * @throws IllegalStateException if no binding is found for the given state
     */
    <T> ModelBinding<T> getBinding(Property property, WidgetState state);

    /**
     * Returns the binding that connects the font face model to this widget.
     *
     * @return the font face model binding
     */
    default ModelBinding<FontFace> getFontFaceModelBinding() {
        return this.getBinding(Property.FONT_FACE, WidgetState.NORMAL);
    }

    /**
     * Returns the current font face.
     *
     * @return the current font face
     */
    default FontFace getFontFace() {
        return this.getFontFaceModelBinding().getModel().getData();
    }

    /**
     * Updates the font face in the model.
     *
     * @param face the new font face
     */
    default void setFontFace(final FontFace face) {
        this.getFontFaceModelBinding().getModel().setData(face);
    }

    /**
     * Listener that listens to font face models and sends a "set font face" update.
     */
    final class FontFaceModelListener implements Listener<FontFace> {
        private final Widget widget;

        public FontFaceModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void accept(final FontFace data) {
            this.widget.pushUpdate(new SetFontFace(this.widget.getId(), data));
        }
    }

    /**
     * Returns the binding that connects the font face size to this widget.
     *
     * @return the font face size binding
     */
    default ModelBinding<FontSize> getFontSizeModelBinding() {
        return this.getBinding(Property.FONT_SIZE, WidgetState.NORMAL);
    }

    default FontSize getFontSize() {
        return this.getFontSizeModelBinding().getModel().getData();
    }

    default void setFontSize(final FontSize size) {
        this.getFontSizeModelBinding().getModel().setData(size);
    }

    final class FontSizeModelListener implements Listener<FontSize> {
        private final Widget widget;

        public FontSizeModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void accept(final FontSize data) {
            this.widget.pushUpdate(new SetFontSize(this.widget.getId(), data));
        }
    }

    /**
     * Returns the binding that connects the font weight model to this widget.
     *
     * @return the font weight model binding
     */
    default ModelBinding<FontWeight> getFontWeightModelBinding() {
        return this.getBinding(Property.FONT_WEIGHT, WidgetState.NORMAL);
    }

    default FontWeight getFontWeight() {
        return this.getFontWeightModelBinding().getModel().getData();
    }

    default void setFontWeight(final FontWeight weight) {
        this.getFontWeightModelBinding().getModel().setData(weight);
    }

    final class FontWeightModelListener implements Listener<FontWeight> {
        private final Widget widget;

        public FontWeightModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void accept(final FontWeight data) {
            this.widget.pushUpdate(new SetFontWeight(this.widget.getId(), data));
        }
    }

    /**
     * Returns the binding that connects the italic (boolean) model to this widget.
     *
     * @return the italic (boolean) model binding
     */
    default ModelBinding<Boolean> getItalicModelBinding() {
        return this.getBinding(Property.ITALIC, WidgetState.NORMAL);
    }

    default boolean isItalic() {
        return this.getItalicModelBinding().getModel().getData();
    }

    default void setItalic(final boolean italic) {
        this.getItalicModelBinding().getModel().setData(italic);
    }

    final class ItalicModelListener implements Listener<Boolean> {
        private final Widget widget;

        public ItalicModelListener(final Widget widget) {
            this.widget = widget;
        }

        @Override
        public void accept(final Boolean data) {
            this.widget.pushUpdate(new SetItalic(this.widget.getId(), data));
        }
    }
}
