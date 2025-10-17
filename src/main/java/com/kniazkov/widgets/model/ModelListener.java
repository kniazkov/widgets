/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.view.Entity;

/**
 * A specialized {@link Listener} implementation bound to a specific {@link Entity}.
 * A {@code ModelListener} represents a reactive link between a {@link Model} and an {@link Entity}
 * that should respond to model updates. Each listener instance is permanently associated
 * with a single target entity, which typically corresponds to a widget or style element affected
 * by the model’s data. The listener can produce equivalent instances bound to different targets
 * via {@link #create(Entity)}, allowing reuse of the same reactive behavior pattern across
 * multiple entities.
 *
 * @param <T> the type of the data received from the model
 */
public abstract class ModelListener<T> implements Listener<T> {
    /**
     * The target entity that this listener is bound to.
     */
    private final Entity target;

    /**
     * Creates a new listener bound to the specified target entity.
     *
     * @param target the entity to associate this listener with (must not be {@code null})
     */
    public ModelListener(final Entity target) {
        this.target = target;
    }

    /**
     * Returns the entity currently associated with this listener.
     *
     * @return the target entity
     */
    public Entity getTarget() {
        return this.target;
    }

    /**
     * Creates a new listener of the same type and behavior, bound to a different target entity.
     * Implementations should return a new instance that reacts identically to model updates
     * but directs them to the specified {@code target}. This mechanism is typically used when
     * duplicating styles or widgets that share model listeners.
     *
     * @param target the entity to associate with the new listener
     * @return a new listener instance bound to {@code target}
     */
    public abstract ModelListener<T> create(final Entity target);
}
