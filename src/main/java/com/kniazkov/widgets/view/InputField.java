/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.common.InlineWidgetSize;
import com.kniazkov.widgets.controller.PointerEvent;
import com.kniazkov.widgets.controller.ProcessesPointerEvents;
import com.kniazkov.widgets.controller.TypedController;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.ColorModel;
import com.kniazkov.widgets.model.FontFaceModel;
import com.kniazkov.widgets.model.FontSizeModel;
import com.kniazkov.widgets.model.FontWeightModel;
import com.kniazkov.widgets.model.InlineWidgetSizeModel;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.Binding;
import java.util.Optional;

/**
 * An editable text input field widget.
 */
public class InputField extends InlineWidget implements HasTextInput, HasStyledText, HasColor,
    HasBgColor, HasWidth<InlineWidgetSize>, HasHeight<InlineWidgetSize>, ProcessesPointerEvents {

    /**
     * Binding between the text model and this widget.
     */
    final Binding<String> text;

    /**
     * Binding between the foreground (text) color model and this widget.
     */
    final Binding<Color> color;

    /**
     * Binding between the background color model and this widget.
     */
    final Binding<Color> bgColor;

    /**
     * Binding between the font face model and this widget.
     */
    final Binding<FontFace> fontFace;

    /**
     * Binding between the font size model and this widget.
     */
    final Binding<FontSize> fontSize;

    /**
     * Binding between the font weight model and this widget.
     */
    final Binding<FontWeight> fontWeight;

    /**
     * Binding between the italic model and this widget.
     */
    final Binding<Boolean> italic;

    /**
     * Binding between the width model and this widget.
     */
    final Binding<InlineWidgetSize> width;

    /**
     * Binding between the height model and this widget.
     */
    final Binding<InlineWidgetSize> height;

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
        this.text = new Binding<>(
            new StringModel(text),
            new TextModelListener(this)
        );
        this.color = new Binding<>(
            new ColorModel(),
            new ColorModelListener(this)
        );
        final Model<Color> bgColorModel = new ColorModel();
        bgColorModel.setData(Color.WHITE);
        this.bgColor = new Binding<>(
            bgColorModel,
            new BgColorModelListener( this, State.NORMAL)
        );
        this.fontFace = new Binding<>(
            new FontFaceModel(),
            new FontFaceModelListener(this)
        );
        this.fontSize = new Binding<>(
            new FontSizeModel(),
            new FontSizeModelListener(this)
        );
        this.fontWeight = new Binding<>(
            new FontWeightModel(),
            new FontWeightModelListener(this)
        );
        this.italic = new Binding<>(
            new BooleanModel(),
            new ItalicModelListener(this)
        );
        this.width = new Binding<>(
            new InlineWidgetSizeModel(),
            new WidthModelListener<>(this)
        );
        this.height = new Binding<>(
            new InlineWidgetSizeModel(),
            new HeightModelListener<>(this)
        );
        this.textInputCtrl = TypedController.stub();
        this.clickCtrl = TypedController.stub();
        this.mouseOverCtrl = TypedController.stub();
        this.mouseOutCtrl = TypedController.stub();
    }

    @Override
    public Binding<Color> getColorModelBinding() {
        return this.color;
    }

    @Override
    public Binding<FontFace> getFontFaceModelBinding() {
        return this.fontFace;
    }

    @Override
    public Binding<FontSize> getFontSizeModelBinding() {
        return this.fontSize;
    }

    @Override
    public Binding<FontWeight> getFontWeightModelBinding() {
        return this.fontWeight;
    }

    @Override
    public Binding<Boolean> getItalicModelBinding() {
        return this.italic;
    }

    @Override
    public Binding<InlineWidgetSize> getWidthModelBinding() {
        return this.width;
    }

    @Override
    public Binding<InlineWidgetSize> getHeightModelBinding() {
        return this.height;
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
                this.text.getModel().setData(value);
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
