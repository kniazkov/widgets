/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        this.setChild(new TextWidget());
        this.clickCtrl = StubController.INSTANCE;
    }

    /**
     * Constructor that creates a button with text.
     * @param text Text
     */
    public Button(final String text) {
        this.setChild(new TextWidget(text));
        this.clickCtrl = StubController.INSTANCE;
    }

    @Override
    public boolean accept(final @NotNull WidgetVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    @NotNull String getType() {
        return "button";
    }

    @Override
    public InlineWidget getChild() {
        return this.child;
    }

    /**
     * Sets the child widget
     * @param child Child widget
     */
    public void setChild(@NotNull InlineWidget child) {
        this.child = child;
        this.sendToClient(new SetChild(this.getWidgetId(), child.getWidgetId()));
    }

    @Override
    void handleEvent(final @NotNull String type, final @Nullable JsonObject data) {
        if (type.equals("click")) {
            this.clickCtrl.handleEvent();
        }
    }

    @Override
    public void onClick(@NotNull Controller ctrl) {
        this.clickCtrl = ctrl;
    }
}
