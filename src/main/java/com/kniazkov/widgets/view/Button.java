/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.PointerEvent;
import com.kniazkov.widgets.controller.ProcessesPointerEvents;
import com.kniazkov.widgets.controller.TypedController;
import com.kniazkov.widgets.protocol.SetWidgetInContainer;
import java.util.Optional;

/**
 * A clickable button widget that decorates a single {@link InlineWidget}.
 */
public class Button extends InlineWidget
        implements Decorator<InlineWidget>, ProcessesPointerEvents {
    /**
     * Decorated widget (displayed inside the button).
     */
    private InlineWidget child;

    /**
     * Controller that handles pointer click events.
     */
    private TypedController<PointerEvent> clickCtrl;

    /**
     * Controller that handles pointer enter (hover) events.
     */
    private TypedController<PointerEvent> mouseOverCtrl;

    /**
     * Controller that handles pointer leave (unhover) events.
     */
    private TypedController<PointerEvent> mouseOutCtrl;

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
        this.child = new TextWidget(text);
        this.pushUpdate(new SetWidgetInContainer(this.child.getId(), this.getId()));
        this.clickCtrl = PointerEvent.STUB_CONTROLLER;
        this.mouseOverCtrl = PointerEvent.STUB_CONTROLLER;
        this.mouseOutCtrl = PointerEvent.STUB_CONTROLLER;
    }

    @Override
    public void remove(Widget widget) {
        this.child.setParent(null);
        this.child = new TextWidget();
        this.pushUpdate(new SetWidgetInContainer(this.child.getId(), this.getId()));
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
        this.pushUpdate(new SetWidgetInContainer(this.child.getId(), this.getId()));
    }

    @Override
    public String getType() {
        return "button";
    }

    @Override
    public void handleEvent(final String type, final Optional<JsonObject> data) {
        if (!data.isPresent()) {
            return;
        }
        PointerEvent event = ProcessesPointerEvents.parsePointerEvent(data.get());
        switch (type) {
            case "click":
                this.clickCtrl.handleEvent(event);
                break;
            case "pointer enter":
                this.mouseOverCtrl.handleEvent(event);
                break;
            case "pointer leave":
                this.mouseOutCtrl.handleEvent(event);
                break;
        }
    }
    @Override
    public void onClick(final TypedController<PointerEvent> ctrl) {
        this.clickCtrl = ctrl;
        this.subscribeToEvent("click");
    }

    @Override
    public void onPointerEnter(final TypedController<PointerEvent> ctrl) {
        this.mouseOverCtrl = ctrl;
        this.subscribeToEvent("pointer enter");
    }

    @Override
    public void onPointerLeave(final TypedController<PointerEvent> ctrl) {
        this.mouseOutCtrl = ctrl;
        this.subscribeToEvent("pointer leave");
    }
}
