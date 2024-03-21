/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Button widget.
 */
public final class Button implements Widget, Clickable {
    /**
     * Identifier.
     */
    private final UId id;

    /**
     * Controller that determines the behavior when the button is clicked.
     */
    private Controller clickCtrl;

    /**
     * Constructor.
     */
    public Button() {
        this.id = UId.create();
        this.clickCtrl = StubController.INSTANCE;
    }

    @Override
    public UId getId() {
        return this.id;
    }

    @Override
    public void onClick(@NotNull Controller ctrl) {
        this.clickCtrl = ctrl;
    }

}
