/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Input field widget.
 */
public final class InputField implements Widget, HasText, Clickable {
    /**
     * Identifier.
     */
    private final UId id;

    /**
     * Model that stores and processes the text of this widget.
     */
    private Model<String> textModel;

    /**
     * Controller that determines the behavior when the field is clicked.
     */
    private Controller clickCtrl;

    /**
     * Constructor.
     */
    public InputField() {
        this.id = UId.create();
        this.textModel = new DefaultStringModel();
        this.clickCtrl = StubController.INSTANCE;
    }

    @Override
    public UId getId() {
        return this.id;
    }

    @Override
    public @NotNull Model<String> getTextModel() {
        return this.textModel;
    }

    @Override
    public void setTextModel(@NotNull Model<String> model) {
        this.textModel = model;
    }

    @Override
    public void onClick(@NotNull Controller ctrl) {
        this.clickCtrl = ctrl;
    }
}
