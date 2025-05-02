/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A base class for inline widgets.
 * <p>
 *     An inline widget is a UI element that can appear inside a line of text or be laid out
 *     horizontally alongside other widgets. These widgets typically consume only as much
 *     horizontal space as needed to display their content and are positioned left to right
 *     in a container.
 * </p>
 */
public abstract class InlineWidget extends Widget {

    /**
     * Constructs a new widget and registers it with the associated client.
     * @param client The owning client instance
     * @param parent The container that owns this widget (nullable for root)
     */
    public InlineWidget(Client client, Container parent) {
        super(client, parent);
    }
}
