/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.model.DefaultStringModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.ModelBinding;
import java.util.Optional;

/**
 * A simple text widget.
 */
public class TextWidget extends InlineWidget implements HasText {
    /**
     * Binding between the text model and the widget.
     */
    final ModelBinding<String> text;

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
        final Model<String> textModel = new DefaultStringModel();
        textModel.setData(text);
        this.text = new ModelBinding<>(
            textModel,
            new TextModelListener(this)
        );
    }

    @Override
    public Model<String> getTextModel() {
        return this.text.getModel();
    }

    @Override
    public void setTextModel(Model<String> model) {
        this.text.setModel(model);
    }

    @Override
    public String getType() {
        return "text";
    }

    @Override
    void handleEvent(String type, Optional<JsonObject> data) {
        // no events to process
    }
}
