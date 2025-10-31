/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.PointerEvent;
import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.controller.Controller;
import com.kniazkov.widgets.protocol.SetChild;
import java.util.Optional;

/**
 * A clickable button widget that decorates a single {@link InlineWidget}.
 */
public class Button extends InlineWidget
        implements Decorator<InlineWidget>, HandlesPointerEvents {
    /**
     * Decorated widget (displayed inside the button).
     */
    private InlineWidget child;

    /**
     * Creates a button with an empty text child.
     */
    public Button() {
        this("");
    }

    /**
     * Creates a button with a {@link TextWidget} as its initial child.
     *
     * @param text the button text
     */
    public Button(final String text) {
        super(Style.getEmptyStyle());
        this.child = new TextWidget(text);
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public InlineWidget getChild() {
        return this.child;
    }

    @Override
    public void put(InlineWidget widget) {
        if (this.child == widget) {
            return;
        }
        this.child.setParent(null);
        this.child = widget;
        widget.setParent(this);
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public void remove(Widget widget) {
        this.child.setParent(null);
        this.child = new TextWidget();
        this.pushUpdate(new SetChild(this.child.getId(), this.getId()));
    }

    @Override
    public String getType() {
        return "button";
    }
}
