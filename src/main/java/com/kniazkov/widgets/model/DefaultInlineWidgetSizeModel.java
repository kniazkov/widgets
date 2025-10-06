/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.model;

import com.kniazkov.widgets.common.InlineWidgetSize;

/**
 * A default inline widget size model implementation.
 */
public final class DefaultInlineWidgetSizeModel extends DefaultModel<InlineWidgetSize> {
    @Override
    public InlineWidgetSize getDefaultData() {
        return InlineWidgetSize.UNDEFINED;
    }
}
