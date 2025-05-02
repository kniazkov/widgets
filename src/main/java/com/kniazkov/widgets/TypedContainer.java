/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A specialized container that holds only widgets of a specific type.
 *
 * @param <T> The type of child widgets
 */
public interface TypedContainer<T extends Widget> extends Container {

    /**
     * Returns the list of typed child widgets.
     * <p>
     *     The returned list is immutable and only contains widgets of type {@code T}.
     * </p>
     *
     * @return List of typed children
     */
    List<T> getTypedChildren();

    /**
     * Returns the list of all child widgets (non-generic view).
     * <p>
     *     This method returns the same data as {@link #getTypedChildren()},
     *     but as a {@code List<Widget>}, to satisfy the {@link Container} interface.
     *     The list is always immutable.
     * </p>
     *
     * @return Immutable list of child widgets
     */
    @Override
    default List<Widget> getChildren() {
        return Collections.unmodifiableList(new ArrayList<Widget>(this.getTypedChildren()));
    }
}
