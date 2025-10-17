package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.Listener;
import com.kniazkov.widgets.view.Entity;

public abstract class ModelListener<T> implements Listener<T> {
    private final Entity target;

    public ModelListener(final Entity target) {
        this.target = target;
    }

    public Entity getTarget() {
        return this.target;
    }

    public abstract ModelListener<T> create(final Entity target);
}
