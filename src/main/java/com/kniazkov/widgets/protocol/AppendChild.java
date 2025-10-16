/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.UId;

/**
 * An {@link Update} that instructs the client to add a target to a specific container.
 */
public final class AppendChild extends Update {
    /**
     * The identifier of the container to which the target is added.
     */
    private final UId container;

    /**
     * Creates a new "append child" update.
     *
     * @param target the target being added
     * @param container the container that receives the target
     */
    public AppendChild(final UId target, final UId container) {
        super(target);
        this.container = container;
    }

    @Override
    public Update clone() {
        return new AppendChild(this.getTargetId(), this.container);
    }

    @Override
    protected String getAction() {
        return "append child";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("container", this.container.toString());
    }
}
