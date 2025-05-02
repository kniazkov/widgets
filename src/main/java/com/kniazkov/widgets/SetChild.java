/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;

/**
 * Instruction that sets or replaces the child of a widget.
 * <p>
 *     This instruction is used in single-child containers (such as decorators),
 *     where a new widget should become the sole content of its parent.
 * </p>
 */
final class SetChild extends Instruction {
    /**
     * ID of the widget to be set as the new child.
     */
    private final UId childId;

    /**
     * Constructs a new 'set child' instruction.
     *
     * @param widgetId The ID of the parent widget
     * @param childId  The ID of the widget to assign as child
     */
    SetChild(final UId widgetId, final UId childId) {
        super(widgetId);
        this.childId = childId;
    }

    @Override
    protected String getAction() {
        return "set child";
    }

    @Override
    protected void fillJsonObject(final JsonObject json) {
        json.addString("child", this.childId.toString());
    }
}
