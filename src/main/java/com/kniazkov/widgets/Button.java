/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * Button widget.
 */
public final class Button extends Decorator implements Clickable {
    /**
     * Child widget.
     */
    private Widget child;

    /**
     * Controller that determines the behavior when the button is clicked.
     */
    private Controller clickCtrl;

    /**
     * Constructor.
     */
    public Button() {
        this.child = new Label();
        this.clickCtrl = StubController.INSTANCE;
    }

    @Override
    public Widget getChild() {
        return this.child;
    }

    @Override
    void handleEvent(final JsonObject json, final String type) {
        if (type.equals("click")) {
            this.clickCtrl.handleEvent();
        }
    }

    @Override
    public void onClick(@NotNull Controller ctrl) {
        this.clickCtrl = ctrl;
    }
}
