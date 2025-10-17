/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.protocol.Update;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class Entity {
    public abstract UId getId();

    /**
     * List of updates not yet sent to the client.
     */
    private final List<Update> updates = new ArrayList<>();

    /**
     * Collects and clears all pending updates from this widget, adding them
     * to the given set. This effectively drains the update queues.
     *
     * @param set the set to which updates are added
     */
    public void getUpdates(final Set<Update> set) {
        set.addAll(this.updates);
        this.updates.clear();
    }

    /**
     * Adds an update for this widget.
     *
     * @param update the update to add
     */
    public void pushUpdate(final Update update) {
        this.updates.add(update);
    }
}
