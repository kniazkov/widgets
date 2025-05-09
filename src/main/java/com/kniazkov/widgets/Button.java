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
 *     (typically text), and invokes a configured {@link Controller} when clicked.
 * </p>
 *
 * <p>
 *     By default, the button is initialized with an empty {@link TextWidget} as its content,
 *     and a {@link StubController} that does nothing. The content can be replaced, and the click
 *     handler can be customized using {@link #onClick(Controller)}.
 * </p>
 */
public final class Button extends InlineWidget implements Decorator<InlineWidget>, Clickable {
    /**
     * Child widget rendered inside the button.
     */
    private InlineWidget child;

    /**
     * Controller invoked when the button is clicked.
     */
    private Controller clickCtrl;

    /**
     * Constructs a new button with default text content and no-op click behavior.
     *
     * @param client The owning client
     * @param parent The parent container
     */
    Button(final Client client, final Container parent) {
        super(client, parent);
        this.setChild(new TextWidget(client, this));
        this.clickCtrl = StubController.INSTANCE;
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
        if (type.equals("click")) {
            this.clickCtrl.handleEvent();
        }
    }

    @Override
    public InlineWidget getChild() {
        return this.child;
    }

    @Override
    public void onClick(final Controller ctrl) {
        this.clickCtrl = ctrl;
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
}
