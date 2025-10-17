/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.InlineWidgetSize;
import com.kniazkov.widgets.controller.PointerEvent;
import com.kniazkov.widgets.controller.ProcessesPointerEvents;
import com.kniazkov.widgets.controller.TypedController;
import com.kniazkov.widgets.model.DefaultStringModel;
import java.util.Optional;

/**
 * An editable text input field widget.
 */
public class InputField extends InlineWidget implements HasTextInput, HasStyledText, HasColor,
        HasBgColor, HasWidth<InlineWidgetSize>, HasHeight<InlineWidgetSize>,
        ProcessesPointerEvents {
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
        this.setStyle(InputFieldStyle.DEFAULT);
        this.bind(
            Property.TEXT,
            new DefaultStringModel(text),
            new TextModelListener(this),
            DefaultStringModel.FACTORY
        );
        this.textInputCtrl = TypedController.stub();
        this.clickCtrl = TypedController.stub();
        this.mouseOverCtrl = TypedController.stub();
        this.mouseOutCtrl = TypedController.stub();
    }

    /**
     * Sets the style of the input field.
     *
     * @param style new style of input field
     */
    public void setStyle(final InputFieldStyle style) {
        super.setStyle(style);
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
}
