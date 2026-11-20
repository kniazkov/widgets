/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.List;

/**
 * Native multiple selection backed by stable keys rather than option positions.
 * @param <T> application value type
 */
public final class MultiSelect<T> extends TypedChoice<T> {
    /**
     * @param options initial options
     */
    public MultiSelect(final List<ChoiceOption<T>> options) {
        super(true, options);
        this.publishChoices();
    }

    @Override
    public String getType() {
        return "typed multi select";
    }
}
