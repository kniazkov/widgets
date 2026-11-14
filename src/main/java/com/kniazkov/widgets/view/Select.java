/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.List;

/**
 * Native single selection backed by stable keys rather than option positions.
 * @param <T> application value type
 */
public final class Select<T> extends TypedChoice<T> {
    /**
     * @param options initial options
     */
    public Select(final List<ChoiceOption<T>> options) {
        super(false, options);
        this.publishChoices();
    }

    @Override
    public String getType() {
        return "typed select";
    }
}
