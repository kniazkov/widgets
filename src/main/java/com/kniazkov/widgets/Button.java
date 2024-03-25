/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * Button widget.
 */
public final class Button extends InlineWidget implements Decorator<InlineWidget>, Clickable {
    /**
     * Child widget.
     */
    private InlineWidget child;

    /**
     * Controller that determines the behavior when the button is clicked.
     */
    private Controller clickCtrl;

    /**
     * Constructor.
     */
    public Button() {
        this.child = new TextWidget();
        this.clickCtrl = StubController.INSTANCE;
    }

    @Override
    public InlineWidget getChild() {
        return this.child;
    }

    public void setChild(@NotNull InlineWidget child) {
        this.child = child;
    }

    @Override
    void handleEvent(final JsonObject json, final @NotNull String type) {
        if (type.equals("click")) {
            this.clickCtrl.handleEvent();
        }
    }

    @Override
    public void onClick(@NotNull Controller ctrl) {
        this.clickCtrl = ctrl;
    }
}
