/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.UId;
import com.kniazkov.widgets.protocol.Update;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base class for any identifiable reactive entity within the system.
 * An {@code Entity} represents a server-side object that has a unique {@link UId identifier}
 * and participates in the reactive update cycle. Entities may correspond to UI widgets, styles,
 * or other application-level components that can change state and send updates to clients.
 */
public abstract class Entity {
    /**
     * List of updates not yet sent to the client.
     */
    private final List<Update> updates = new ArrayList<>();

    /**
     * Returns the unique identifier of this entity.
     *
     * @return the unique identifier of this entity
     */
    public abstract UId getId();

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
