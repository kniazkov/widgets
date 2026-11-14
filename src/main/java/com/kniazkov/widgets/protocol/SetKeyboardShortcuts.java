/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonArray;
import com.kniazkov.json.JsonObject;
import com.kniazkov.json.JsonString;
import com.kniazkov.widgets.common.RMId;
import java.util.List;

/**
 * Replaces the browser-side editor keyboard subscription table in one update.
 */
public final class SetKeyboardShortcuts extends Update {
    /**
     * Immutable shortcut snapshot.
     */
    private final List<String> shortcuts;

    /**
     * Creates a shortcut-list update.
     *
     * @param widget target widget
     * @param shortcuts ordered shortcut snapshot
     */
    public SetKeyboardShortcuts(final RMId widget, final List<String> shortcuts) {
        super(widget);
        this.shortcuts = List.copyOf(shortcuts);
    }

    @Override
    public Update clone() {
        return new SetKeyboardShortcuts(this.getWidgetId(), this.shortcuts);
    }

    @Override
    protected String getAction() {
        return "set keyboard shortcuts";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        final JsonArray array = json.createArray("shortcuts");
        for (final String shortcut : this.shortcuts) {
            array.add(new JsonString(shortcut));
        }
    }
}
