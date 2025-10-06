/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * Abstract base class for UI updates sent from the server to the client. An {@code Update}
 * represents a change in the state of a specific widget that must be reflected in the client view.
 * Each update is uniquely identified by an auto-generated {@link UId}, which allows updates
 * to be compared and applied in a strict chronological order.
 */
public abstract class Update implements Comparable<Update> {
    /**
     * Update Id.
     */
    private final UId id;

    /**
     * The Id of the widget to which the update applies.
     */
    private final UId widget;

    /**
     * Creates a new update for the specified widget.
     *
     * @param widget the target widget identifier
     */
    protected Update(final UId widget) {
        this.id = UId.create();
        this.widget = widget;
    }

    /**
     * Returns the unique identifier of this update.
     *
     * @return the update ID
     */
    public UId getId() {
        return this.id;
    }

    /**
     * Serializes this update into a JSON object.
     *
     * @param obj the JSON object to fill
     */
    public void serialize(final JsonObject obj) {
        obj.addString("id", this.id.toString());
        obj.addString("widget", this.widget.toString());
        obj.addString("action", this.getAction());
        this.fillJsonObject(obj);
    }

    /**
     * Returns the action type of this update.
     *
     * @return the action string
     */
    protected abstract String getAction();


    /**
     * Allows subclasses to add extra fields to the JSON representation.
     * Default implementation does nothing.
     *
     * @param json the JSON object to extend
     */
    protected void fillJsonObject(final JsonObject json) {
        // default: no additional fields
    }

    @Override
    public String toString() {
        final JsonObject obj = new JsonObject();
        this.serialize(obj);
        return obj.toString();
    }

    @Override
    public int compareTo(final Update other) {
        return this.id.compareTo(other.id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj instanceof Update) {
            return this.id.equals(((Update) obj).id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
