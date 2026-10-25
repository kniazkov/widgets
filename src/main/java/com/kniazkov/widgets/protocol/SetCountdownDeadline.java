/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.protocol;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;

/**
 * Replaces an absolute countdown deadline; server time comes from the response envelope.
 */
public final class SetCountdownDeadline extends Update {
    /**
     * Epoch milliseconds of the deadline.
     */
    private final long deadline;

    /**
     * Creates a deadline update.
     *
     * @param widget target countdown
     * @param deadline epoch milliseconds
     */
    public SetCountdownDeadline(final RMId widget, final long deadline) {
        super(widget);
        this.deadline = deadline;
    }

    @Override
    public Update clone() {
        return new SetCountdownDeadline(this.getWidgetId(), this.deadline);
    }

    @Override
    protected String getAction() {
        return "set countdown deadline";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addNumber("deadline", this.deadline);
    }
}
