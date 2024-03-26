/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * 'Set child' instruction.
 */
final class SetChild extends Instruction {
    /**
     * Widget id to be set.
     */
    private final UId childId;

    /**
     * Constructor.
     *
     * @param widgetId Identifier of the widget the instruction is working with
     * @param childId Widget id to be set
     */
    SetChild(final @NotNull UId widgetId, final @NotNull UId childId) {
        super(widgetId);
        this.childId = childId;
    }

    @Override
    protected @NotNull String getAction() {
        return "set child";
    }

    @Override
    protected void fillJsonObject(final @NotNull JsonObject json) {
        json.addString("child", this.childId.toString());
    }
}
