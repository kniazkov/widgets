/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.List;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class describing a client instance.
 */
final class Client implements Comparable<Client> {
    /**
     * Identifier.
     */
    private final UId id;

    /**
     * Timer (in ms). When it expires, the client will be destroyed.
     */
    long timer;

    /**
     * Root widget.
     */
    private final RootWidget root;

    /**
     * Constructor.
     */
    Client() {
        this.id = UId.create();
        this.root = new RootWidget();
    }

    /**
     * Returns client's unique identifier.
     * @return Client's identifier.
     */
    UId getId() {
        return this.id;
    }

    /**
     * Returns root widget.
     * @return Root widget
     */
    RootWidget getRootWidget() {
        return this.root;
    }

    /**
     * Handles event that was sent by web page.
     * @param widgetId Widget id
     * @param type Event type
     * @param data Event-related data
     */
    void handleEvent(final UId widgetId, final @NotNull String type, final @Nullable JsonObject data) {
        DeepFirst.traverse(this.root, widget -> {
            if (widget.getWidgetId().equals(widgetId)) {
                widget.handleEvent(type, data);
                return false;
            }
            return true;
        });
    }

    @Override
    public int compareTo(@NotNull Client other) {
        return this.id.compareTo(other.id);
    }
}
