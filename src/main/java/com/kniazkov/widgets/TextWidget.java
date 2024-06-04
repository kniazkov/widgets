/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Widget that contains plain text.
 */
public final class TextWidget extends InlineWidget implements HasText, HasColor {
    /**
     * Model that stores and processes the text of this widget.
     */
    private Model<String> textModel;

    /**
     * Listener to follow text model data updates and send instructions to clients.
     */
    private final Listener<String> textModelListener;

    /**
     * Model that stores and processes the color of this widget.
     */
    private Model<Color> colorModel;

    /**
     * Listener to follow color model data updates and send instructions to clients.
     */
    private final Listener<Color> colorModelListener;

    /**
     * Constructor.
     */
    public TextWidget() {
        this.textModel = new DefaultStringModel();
        this.textModelListener = new TextModelListener(this);
        this.textModel.addListener(this.textModelListener);

        this.colorModel = new DefaultColorModel();
        this.colorModelListener = new ColorModelListener(this);
        this.colorModel.addListener(this.colorModelListener);
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
    public boolean accept(final @NotNull WidgetVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    @NotNull String getType() {
        return "text";
    }

    @Override
    void handleEvent(final @NotNull String type, final @Nullable JsonObject data) {
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
        this.textModel.notifyListeners();
    }

    @Override
    public @NotNull Model<Color> getColorModel() {
        return this.colorModel;
    }

    @Override
    public void setColorModel(@NotNull Model<Color> model) {
        this.colorModel.removeListener(this.colorModelListener);
        this.colorModel = model;
        this.colorModel.addListener(this.colorModelListener);
        this.colorModel.notifyListeners();
    }
}
