/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import java.util.Optional;

/**
 * A button widget.
 * <p>
 *     Represents a clickable inline UI element that contains a single child widget
 *     (typically text), and invokes a configured {@link TypedController} when clicked.
 * </p>
 *
 * <p>
 *     By default, the button is initialized with an empty {@link TextWidget} as its content.
 * </p>
 */
public final class Button extends InlineWidget implements Decorator<InlineWidget>, HasBgColor,
    HasWidth<InlineWidgetSize>, ProcessesPointerEvents {
    /**
     * Child widget rendered inside the button.
     */
    private InlineWidget child;

    /**
     * Background color model binding — handles model and listener for color changes.
     */
    private final ModelBinding<Color> bgColor;

    /**
     * Controller invoked when the button is clicked.
     */
    private TypedController<PointerEvent> clickCtrl;

    private TypedController<PointerEvent> mouseOverCtrl;

    private TypedController<PointerEvent> mouseOutCtrl;

    /**
     * Width model binding — handles model and listener for width changes.
     */
    private final ModelBinding<InlineWidgetSize> width;

    /**
     * Constructs a new button with default text content and no-op click behavior.
     *
     * @param client The owning client
     * @param parent The parent container
     */
    Button(final Client client, final Container parent) {
        super(client, parent);
        this.setChild(new TextWidget(client, this));
        final StyleSet styles = client.getRootWidget().getDefaultStyles();
        final ButtonStyle style = styles.getDefaultButtonStyle();
        this.bgColor = new ModelBinding<>(
            style.getBackgroundColorModel().fork(),
            new BgColorModelListener(this)
        );
        this.clickCtrl = PointerEvent.STUB_CONTROLLER;
        this.mouseOverCtrl = PointerEvent.STUB_CONTROLLER;
        this.mouseOutCtrl = PointerEvent.STUB_CONTROLLER;
        this.width = new ModelBinding<>(
            new DefaultInlineWidgetSizeModel(),
            new WidgetSizeListener<InlineWidgetSize>(this, "width")
        );
    }

    @Override
    public boolean accept(final WidgetVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    String getType() {
        return "button";
    }

    @Override
    void handleEvent(final String type, final Optional<JsonObject> data) {
        if (!data.isPresent()) {
            return;
        }
        if (type.equals("click")) {
            this.clickCtrl.handleEvent(ProcessesPointerEvents.parsePointerEvent(data.get()));
        } else if (type.equals("pointer enter")) {
            this.mouseOverCtrl.handleEvent(ProcessesPointerEvents.parsePointerEvent(data.get()));
        } else if (type.equals("pointer leave")) {
            this.mouseOutCtrl.handleEvent(ProcessesPointerEvents.parsePointerEvent(data.get()));
        }
    }

    @Override
    public InlineWidget getChild() {
        return this.child;
    }

    @Override
    public void remove(Widget widget) {
        if (widget == this.child) {
            this.child = new TextWidget(this.getClient(), this);
        }
    }

    /**
     * Sets the button's text content if its child supports {@link HasText}.
     *
     * @param text The new text
     */
    public void setText(final String text) {
        if (this.child instanceof HasText) {
            ((HasText) this.child).setText(text);
        }
    }

    /**
     * Replaces the button's content with a new {@link TextWidget}.
     *
     * @return The newly created {@link TextWidget}
     */
    public TextWidget createTextWidget() {
        final TextWidget widget = new TextWidget(this.getClient(), this);
        this.setChild(widget);
        return widget;
    }

    /**
     * Replaces the button's content with a new {@link TextWidget} containing the given text.
     *
     * @param text The initial text
     * @return The created {@link TextWidget}
     */
    public TextWidget createText(final String text) {
        final TextWidget widget = new TextWidget(this.getClient(), this, text);
        this.setChild(widget);
        return widget;
    }

    /**
     * Replaces the current child widget and sends a {@code SetChild} instruction to the client.
     *
     * @param child The new child widget
     */
    private void setChild(final InlineWidget child) {
        this.child = child;
        this.sendToClient(new SetChild(this.getWidgetId(), child.getWidgetId()));
    }

    @Override
    public Model<Color> getBackgroundColorModel() {
        return this.bgColor.getModel();
    }

    @Override
    public void setBackgroundColorModel(final Model<Color> model) {
        this.bgColor.setModel(model);
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
    public Model<InlineWidgetSize> getWidthModel() {
        return this.width.getModel();
    }

    @Override
    public void setWidthModel(final Model<InlineWidgetSize> model) {
        this.width.setModel(model);
    }

    /**
     * Sets the width of the widget in pixels.
     * <p>
     *     This is a convenience method that wraps the given pixel value in an
     *     {@link InlineWidgetSize} and updates the associated width model.
     * </p>
     *
     * @param pixels The width in pixels (must be ≥ 0)
     */
    public void setWidth(final int pixels) {
        this.width.getModel().setData(new InlineWidgetSize(pixels));
    }
}
