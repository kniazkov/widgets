/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Button widget.
 */
public final class Button extends Widget implements Clickable {
    /**
     * Controller that determines the behavior when the button is clicked.
     */
    private Controller clickCtrl;

    /**
     * Constructor.
     */
    public Button() {
        this.clickCtrl = StubController.INSTANCE;
    }

    @Override
    public void onClick(@NotNull Controller ctrl) {
        this.clickCtrl = ctrl;
    }

    @Override
    public void clicked() {
        this.clickCtrl.handleEvent();
    }
}
