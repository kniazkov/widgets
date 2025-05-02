/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A base class for block-level widgets.
 * <p>
 *     A block widget is a UI element that occupies the full horizontal width of its parent
 *     container. Such widgets are typically arranged in a vertical stack inside their parent
 *     (e.g., top to bottom).
 * </p>
 */
public abstract class BlockWidget extends Widget {
    /**
     * Constructs a new widget and registers it with the associated client.
     * @param client The owning client instance
     * @param parent The container that owns this widget (nullable for root)
     */
    public BlockWidget(final Client client, final Container parent) {
        super(client, parent);
    }
}
