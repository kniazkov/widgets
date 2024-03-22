/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * Input field widget.
 */
public final class InputField extends Widget implements HasTextInput, Clickable {
    /**
     * Model that stores and processes the text of this widget.
     */
    private Model<String> textModel;

    private final ModelListener<String> textModelListener;

    /**
     * Controller that determines the behavior when the field is clicked.
     */
    private TypedController<String> textInputCtrl;

    /**
     * Controller that determines the behavior when the field is clicked.
     */
    private Controller clickCtrl;

    /**
     * Constructor.
     */
    public InputField() {
        this.textModel = new DefaultStringModel();
        this.textModelListener = new HasText.TextModelListener(this);
        this.textModel.addListener(this.textModelListener);
        this.textInputCtrl = data -> { };
        this.clickCtrl = StubController.INSTANCE;
    }

    @Override
    void handleEvent(final JsonObject json, final String type) {
        if (type.equals("text input")) {
            final JsonElement element = json.getElement("text");
            if (element.isString()) {
                final String text = element.getStringValue();
                this.textModel.setData(text);
                this.textInputCtrl.handleEvent(text);
            }
        } else if (type.equals("click")) {
            this.clickCtrl.handleEvent();
        }
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

    @Override
    public void onTextInput(@NotNull TypedController<String> ctrl) {
        this.textInputCtrl = ctrl;
    }

    @Override
    public void onClick(@NotNull Controller ctrl) {
        this.clickCtrl = ctrl;
    }
}
