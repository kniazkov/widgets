/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * A block-level widget that displays a paragraph consisting of text and inline elements.
 * <p>
 *     This widget acts as a {@link BlockWidget} and implements {@link InlineContainer},
 *     allowing it to hold and manage a sequence of {@link InlineWidget} instances
 *     (e.g. text, buttons, inputs). Widgets are laid out horizontally, in a flow-like manner,
 *     and rendered top-to-bottom as a block.
 * </p>
 */
public final class Paragraph extends BlockWidget implements InlineContainer {
    /**
     * Children widgets.
     */
    private final List<InlineWidget> children;

    /**
     * Constructs a new widget and registers it with the associated client.*
     * @param client The owning client instance
     * @param parent The container that owns this widget (nullable for root)
     */
    public Paragraph(Client client, Container parent) {
        super(client, parent);
        this.children = new LinkedList<>();
    }

    @Override
    public boolean accept(final WidgetVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    String getType() {
        return "paragraph";
    }

    @Override
    void handleEvent(final String type, final Optional<JsonObject> data) {
        // do nothing for now
    }

    @Override
    public List<InlineWidget> getTypedChildren() {
        return Collections.unmodifiableList(new ArrayList<>(this.children));
    }

    @Override
    public void remove(final Widget widget) {
        this.children.remove(widget);
    }

    @Override
    public TextWidget createTextWidget(final String text) {
        final TextWidget widget = new TextWidget(this.getClient(), this, text);
        this.appendChild(widget);
        return widget;
    }

    @Override
    public InputField createInputField() {
        final InputField widget = new InputField(this.getClient(), this);
        this.appendChild(widget);
        return widget;
    }

    @Override
    public Button createButton() {
        final Button widget = new Button(this.getClient(), this);
        this.appendChild(widget);
        return widget;
    }

    /**
     * Adds a new child widget and sends an update to the client.
     *
     * @param child The inline widget to add
     */
    private void appendChild(final InlineWidget child) {
        this.children.add(child);
        this.sendToClient(new AppendChild(this.getWidgetId(), child.getWidgetId()));
    }
}
