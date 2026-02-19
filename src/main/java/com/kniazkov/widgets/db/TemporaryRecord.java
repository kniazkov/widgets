/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.model.Model;
import java.time.Instant;
import java.util.UUID;

/**
 * A mutable, provisional view of a {@link Record} used for staged editing.
 * <p>
 * A {@code TemporaryRecord} starts as a shallow, cascading clone of a “parent” {@link Record}.
 * Each field model is wrapped using cascading model meaning:
 *
 * <ul>
 *     <li>the temporary record initially reads values from the parent,</li>
 *     <li>but the first write to any field forks the model into an independent copy,</li>
 *     <li>allowing modifications without immediately affecting the parent.</li>
 * </ul>
 *
 * <p>
 * This enables safe editing sessions, UI dialogs, transactional user actions, and any workflow
 * where changes need to be applied only after explicit confirmation.
 * <br>
 * When {@link #save()} is called, all modified fields are merged back into the parent.
 */
class TemporaryRecord extends Record {
    /**
     * The original record from which this temporary record was created.
     */
    private final Record parent;

    /**
     * Creates a new temporary record derived from the given parent record.
     *
     * @param parent the record to derive from
     */
    public TemporaryRecord(final Record parent) {
        super(Instant.now());
        this.parent = parent;

        for (final String key : parent.data.keySet()) {
            Model<?> model = parent.data.get(key);
            this.data.put(key, model.asCascading());
        }
    }

    @Override
    public UUID getId() {
        return this.parent.getId();
    }

    /**
     * Writes all changes made in this temporary record back into the parent record.
     */
    @Override
    public void save() {
        for (final String key : this.data.keySet()) {
            Model<?> src = this.data.get(key);
            Model<?> dst = this.parent.data.get(key);
            if (dst == null) {
                this.parent.data.put(key, src);
            } else {
                dst.setObject(src.getData());
            }
        }
        this.parent.save();
    }
}
