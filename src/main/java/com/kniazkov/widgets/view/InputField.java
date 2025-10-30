/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.controller.PointerEvent;
import com.kniazkov.widgets.controller.ProcessesPointerEvents;
import com.kniazkov.widgets.controller.TypedController;
import java.util.Optional;

/**
 * An editable text input field widget.
 */
public class InputField extends InlineWidget implements HasTextInput, HasStyledText, HasColor,
        HasBgColor, ProcessesPointerEvents {

    /**
     * Returns the default BaseStyle instance used by text widgets.
     *
     * @return the singleton default {@link TextWidgetStyle} instance
     */
    public static InputFieldStyle getDefaultStyle() {
        return InputFieldStyle.DEFAULT;
    }

    /**
     * Controller that handles incoming text input events from the client.
     */
    private TypedController<String> textInputCtrl;

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
     * Creates a new input field with empty text.
     */
    public InputField() {
        this("");
    }

    /**
     * Creates a new input field with the given initial text.
     *
     * @param text the initial text to display in the input field
     */
    public InputField(final String text) {
        this(getDefaultStyle(), text);
    }

    /**
     * Creates a new input field with the specified style and initial text.
     *
     * @param style the style to apply to this widget
     * @param text the initial text to display
     */
    public InputField(final InputFieldStyle style, final String text) {
        super(style);
        this.setText(text);
        this.textInputCtrl = TypedController.stub();
        this.clickCtrl = TypedController.stub();
        this.mouseOverCtrl = TypedController.stub();
        this.mouseOutCtrl = TypedController.stub();
    }

    @Override
    public void onTextInput(TypedController<String> ctrl) {
        this.textInputCtrl = ctrl;
        // no need to subscribe to this event
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

    @Override
    public String getType() {
        return "input field";
    }

    @Override
    public void handleEvent(String type, Optional<JsonObject> data) {
        if (!data.isPresent()) {
            return;
        }
        switch (type) {
            case "text input":
                final String value = data.get().get("text").getStringValue();
                this.getTextModel().setData(value);
                this.textInputCtrl.handleEvent(value);
                break;
            case "click":
                this.clickCtrl.handleEvent(
                    ProcessesPointerEvents.parsePointerEvent(data.get())
                );
                break;
            case "pointer enter":
                this.mouseOverCtrl.handleEvent(
                    ProcessesPointerEvents.parsePointerEvent(data.get())
                );
                break;
            case "pointer leave":
                this.mouseOutCtrl.handleEvent(
                    ProcessesPointerEvents.parsePointerEvent(data.get())
                );
                break;
        }
    }

    /**
     * Sets a new widget style.
     *
     * @param style new widget style
     */
    public void setStyle(InputFieldStyle style) {
        super.setStyle(style);
    }
}
