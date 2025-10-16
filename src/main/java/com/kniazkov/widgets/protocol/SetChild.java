/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to place (set) a target
 * into a specific container (decorator).
 */
public final class SetChild extends Update {
    /**
     * The identifier of the container where the target is placed.
     */
    private final UId container;

    /**
     * Creates a new "set child" update.
     *
     * @param target the target being placed
     * @param container the container that will contain the target
     */
    public SetChild(final UId target, final UId container) {
        super(target);
        this.container = container;
    }

    @Override
    public Update clone() {
        return new SetChild(this.getTargetId(), this.container);
    }

    @Override
    protected String getAction() {
        return "set child";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("container", this.container.toString());
    }
}
