/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to remove a target from a specific container.
 */
public final class RemoveChild extends Update {
    /**
     * The identifier of the container from which the target is removed.
     */
    private final UId container;

    /**
     * Creates a new "remove child" update.
     *
     * @param target the target being removed
     * @param container the container from which the target is removed
     */
    public RemoveChild(final UId target, final UId container) {
        super(target);
        this.container = container;
    }

    @Override
    public Update clone() {
        return new RemoveChild(this.getTargetId(), this.container);
    }

    @Override
    protected String getAction() {
        return "remove child";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("container", this.container.toString());
    }
}
