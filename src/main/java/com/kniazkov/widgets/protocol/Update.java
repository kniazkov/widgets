/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * Abstract base class for UI updates sent from the server to the client. An {@code Update}
 * represents a change in the state of a specific target that must be reflected in the client view.
 * Each update is uniquely identified by an auto-generated {@link UId}, which allows updates
 * to be compared and applied in a strict chronological order.
 */
public abstract class Update implements Comparable<Update>, Cloneable {
    /**
     * Update ID.
     */
    private final UId id;

    /**
     * The ID of the target to which the update applies.
     */
    private final UId target;

    /**
     * Creates a new update for the specified target.
     *
     * @param target the target identifier
     */
    protected Update(final UId target) {
        this.id = UId.create();
        this.target = target;
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
     * Returns the unique identifier of the target to which the update applies.
     *
     * @return the target ID
     */
    public UId getTargetId() {
        return  this.target;
    }

    /**
     * Serializes this update into a JSON object.
     *
     * @param obj the JSON object to fill
     */
    public void serialize(final JsonObject obj) {
        obj.addString("id", this.id.toString());
        obj.addString("target", this.target.toString());
        obj.addString("action", this.getAction());
        this.fillJsonObject(obj);
    }


    /**
     * Creates an exact copy of this update with a new unique ID.
     * Subclasses must implement this method to return a new instance
     * of the same concrete type, preserving all update-specific data
     * but regenerating its {@link #id}.
     *
     * @return a new {@code Update} instance identical to this one, but with a new {@link UId}
     */
    @Override
    public abstract Update clone();

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
