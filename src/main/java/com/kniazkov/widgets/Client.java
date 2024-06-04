/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

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
     * All widgets by identifiers.
     */
    final Map<UId, Widget> widgets;

    /**
     * Root widget.
     */
    private final RootWidget root;

    /**
     * Constructor.
     */
    Client() {
        this.id = UId.create();
        this.widgets = new TreeMap<>();
        this.root = new RootWidget(this);
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
     * Collects updates from all widgets to send to the web page.
     * @return Sorted list of instructions
     */
    @NotNull List<Instruction> collectUpdates() {
        final Set<Instruction> set = new TreeSet<>();
        for (final Widget widget : widgets.values()) {
            widget.getUpdates(set);
        }
        return new ArrayList<>(set);
    }

    /**
     * Handles event that was sent by web page.
     * @param widgetId Widget id
     * @param type Event type
     * @param data Event-related data
     */
    void handleEvent(final UId widgetId, final @NotNull String type, final @Nullable JsonObject data) {
        final Widget widget = this.widgets.get(widgetId);
        if (widget != null) {
            widget.handleEvent(type, data);
        }
    }

    @Override
    public int compareTo(@NotNull Client other) {
        return this.id.compareTo(other.id);
    }
}
