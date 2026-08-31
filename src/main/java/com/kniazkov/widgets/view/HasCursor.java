/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.Cursor;
import com.kniazkov.widgets.model.Model;

/**
 * An entity with a state-dependent cursor.
 */
public interface HasCursor extends Entity {
    /**
     * Returns the cursor model for a state.
     *
     * @param state widget state
     * @return cursor model
     */
    default Model<Cursor> getCursorModel(final State state) {
        return this.getModel(state, Property.CURSOR);
    }

    /**
     * Sets the cursor model for a state.
     *
     * @param state widget state
     * @param model cursor model
     */
    default void setCursorModel(final State state, final Model<Cursor> model) {
        this.setModel(state, Property.CURSOR, model);
    }

    /**
     * Returns the cursor for a state.
     *
     * @param state widget state
     * @return cursor
     */
    default Cursor getCursor(final State state) {
        return this.getCursorModel(state).getData();
    }

    /**
     * Returns the normal-state cursor.
     *
     * @return cursor
     */
    default Cursor getCursor() {
        return this.getCursor(State.NORMAL);
    }

    /**
     * Sets the cursor for a state.
     *
     * @param state widget state
     * @param cursor cursor
     */
    default void setCursor(final State state, final Cursor cursor) {
        this.getCursorModel(state).setData(cursor);
    }

    /**
     * Sets the same cursor for every supported state.
     *
     * @param cursor cursor
     */
    default void setCursor(final Cursor cursor) {
        for (final State state : this.getSupportedStates()) {
            this.setCursor(state, cursor);
        }
    }
}
