/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * A block of Markdown backed by an ordinary reactive string model.
 * Supports headings, paragraphs, lists, quotes, separators, emphasis, code and links.
 * Raw HTML is displayed literally; images and advanced Markdown extensions are unsupported.
 * Font size is the base size: headings and spacing scale proportionally in the browser.
 */
public final class Markdown extends BlockWidget<MarkdownStyle>
        implements HasStyledText, HasColor, HasMargin, HasPadding, HasHiddenState {
    /**
     * Returns the shared default style.
     *
     * @return default Markdown style
     */
    public static MarkdownStyle getDefaultStyle() {
        return MarkdownStyle.DEFAULT;
    }

    /**
     * Creates an empty document.
     */
    public Markdown() {
        this("");
    }

    /**
     * Creates a document with the default style.
     *
     * @param text Markdown source
     */
    public Markdown(final String text) {
        this(getDefaultStyle(), text);
    }

    /**
     * Creates a document bound to a reactive source.
     *
     * @param model Markdown source model
     */
    public Markdown(final Model<String> model) {
        this(getDefaultStyle(), model);
    }

    /**
     * Creates a styled document.
     *
     * @param style base typography and block spacing
     * @param text Markdown source
     */
    public Markdown(final MarkdownStyle style, final String text) {
        super(style);
        this.setText(text);
    }

    /**
     * Creates a styled document bound to a reactive source.
     *
     * @param style base typography and block spacing
     * @param model Markdown source model
     */
    public Markdown(final MarkdownStyle style, final Model<String> model) {
        super(style);
        this.setTextModel(model);
    }

    @Override
    public String getType() {
        return "markdown";
    }
}
