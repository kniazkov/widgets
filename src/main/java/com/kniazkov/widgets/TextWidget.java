/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * Widget that contains plain text.
 */
public final class TextWidget extends InlineWidget implements HasText {
    /**
     * Model that stores and processes the text of this widget.
     */
    private Model<String> textModel;

    /**
     * Listener to follow text model data updates and send instructions to clients.
     */
    private final ModelListener<String> textModelListener;

    /**
     * Constructor.
     */
    public TextWidget() {
        this.textModel = new DefaultStringModel();
        this.textModelListener = new TextModelListener(this);
        this.textModel.addListener(this.textModelListener);
    }

    @Override
    public boolean accept(final @NotNull WidgetVisitor visitor) {
        return visitor.visit(this);
    }

    /**
     * Constructor that creates a widget with text.
     * @param text Text
     */
    public TextWidget(final @NotNull String text) {
        this();
        this.textModel.setData(text);
    }

    @Override
    void handleEvent(@NotNull JsonObject json, @NotNull String type) {
        // no events supported
    }

    @Override
    public @NotNull Model<String> getTextModel() {
        return this.textModel;
    }

    @Override
    public void setTextModel(@NotNull Model<String> model) {
        this.textModel.removeListener(this.textModelListener);
        this.textModel = model;
        this.textModel.addListener(this.textModelListener);
    }
}
