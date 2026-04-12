package com.kniazkov.widgets.view;

import com.kniazkov.widgets.model.Model;

/**
 * A minimal no-op implementation of {@link Style} that provides default, standalone models
 * but does not store or propagate any state.
 * This style serves as a safe fallback or placeholder for widgets or components
 * that require a style reference but do not yet have a concrete one assigned.
 */
public class EmptyStyle extends Style {
    /**
     * The singleton instance of the empty style.
     */
    public static final EmptyStyle INSTANCE = new EmptyStyle();

    @Override
    public <T> Model<T> getModel(final State state, final Property<T> property) {
        return property.createDefaultModel();
    }

    @Override
    public <T> void setModel(final State state, final Property<T> property,
                             final Model<T> model) {
        // do nothing
    }

    @Override
    public EmptyStyle derive() {
        return this;
    }
}
