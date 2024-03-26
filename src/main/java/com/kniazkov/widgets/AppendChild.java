/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;

/**
 * 'Append child' instruction.
 */
final class AppendChild extends Instruction {
    /**
     * Widget id to be append.
     */
    private final UId childId;

    /**
     * Constructor.
     *
     * @param widgetId Identifier of the widget the instruction is working with
     * @param childId Widget id to be append
     */
    AppendChild(final @NotNull UId widgetId, final @NotNull UId childId) {
        super(widgetId);
        this.childId = childId;
    }

    @Override
    protected @NotNull String getAction() {
        return "append child";
    }

    @Override
    protected void fillJsonObject(final @NotNull JsonObject json) {
        json.addString("child", this.childId.toString());
    }
}
