/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Instruction that appends a child widget to a container.
 * <p>
 *     This instruction is used for containers that support multiple children
 *     (e.g., paragraphs, blocks). The specified widget is added as the last child of the container.
 * </p>
 */
final class AppendChild extends Instruction {
    /**
     * ID of the widget to append.
     */
    private final UId childId;

    /**
     * Constructs a new 'append child' instruction.
     *
     * @param widgetId The ID of the container widget
     * @param childId The ID of the widget to append
     */
    AppendChild(final UId widgetId, final UId childId) {
        super(widgetId);
        this.childId = childId;
    }

    @Override
    protected String getAction() {
        return "append child";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("child", this.childId.toString());
    }
}
