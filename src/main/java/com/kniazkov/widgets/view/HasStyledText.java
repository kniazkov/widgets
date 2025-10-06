/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;
import com.kniazkov.widgets.protocol.Update;

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
     * Returns the binding that connects the font face model to this widget.
     *
     * @return the font face model binding
     */
    ModelBinding<FontFace> getFontFaceModelBinding();

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
            final Update update = new Update(this.widget.getId()) {
                @Override
                protected String getAction() {
                    return "set font face";
                }

                @Override
                protected void fillJsonObject(final JsonObject json) {
                    json.addString("font face", data.getName());
                }
            };
            this.widget.pushUpdate(update);
        }
    }

    ModelBinding<FontSize> getFontSizeModelBinding();

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
            final Update update = new Update(this.widget.getId()) {
                @Override
                protected String getAction() {
                    return "set font size";
                }

                @Override
                protected void fillJsonObject(final JsonObject json) {
                    json.addString("font size", data.getCSSCode());
                }
            };
            this.widget.pushUpdate(update);
        }
    }

    ModelBinding<FontWeight> getFontWeightModelBinding();

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
            final Update update = new Update(this.widget.getId()) {
                @Override
                protected String getAction() {
                    return "set font weight";
                }

                @Override
                protected void fillJsonObject(final JsonObject json) {
                    json.addNumber("font weight", data.getWeight());
                }
            };
            this.widget.pushUpdate(update);
        }
    }

    ModelBinding<Boolean> getItalicModelBinding();

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
            final Update update = new Update(this.widget.getId()) {
                @Override
                protected String getAction() {
                    return "set italic";
                }

                @Override
                protected void fillJsonObject(final JsonObject json) {
                    json.addBoolean("italic", data);
                }
            };
            this.widget.pushUpdate(update);
        }
    }
}
