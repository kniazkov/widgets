/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A standard {@link Model} implementation specialized for {@link InlineWidgetSize} values.
 * <p>
 *     This model inherits behavior from {@link DefaultModel} and performs no validation:
 *     any non-null {@code InlineWidgetSize} is accepted and stored.
 *     It is suitable for managing the dimensions of inline widgets (width or height),
 *     where sizes are represented in absolute CSS-compatible units (e.g., pixels).
 * </p>
 *
 * <p>
 *     The default value returned by {@link #getDefaultData()} is
 *     {@link InlineWidgetSize#UNDEFINED}, which signals that no explicit size has been assigned
 *     and the default browser rendering rules apply.
 * </p>
 */
public final class DefaultInlineWidgetSizeModel extends DefaultModel<InlineWidgetSize> {

    @Override
    protected Model<InlineWidgetSize> createInstance() {
        return new DefaultInlineWidgetSizeModel();
    }

    @Override
    public InlineWidgetSize getDefaultData() {
        return InlineWidgetSize.UNDEFINED;
    }
}
