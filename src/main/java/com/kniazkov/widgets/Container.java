/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.List;

/**
 * Interface representing an entity that contains child widgets.
 * <p>
 *     A container manages a collection of widgets and defines how they are arranged, accessed,
 *     and removed. Examples of containers include panels, layout boxes, or decorators.
 * </p>
 *
 * <p>
 *     The list returned by {@link #getChildren()} is guaranteed to be immutable and reflects
 *     the current state of the container at the time of the call. Modifying the container's
 *     contents must be done through defined API methods.
 * </p>
 */
public interface Container {
    /**
     * Returns the list of child widgets currently held by this container.
     * <p>
     *     The returned list is immutable and safe to iterate. Any structural changes must
     *     be performed via container methods.
     * </p>
     *
     * @return Immutable list of child widgets
     */
    List<Widget> getChildren();

    /**
     * Removes a child widget from the container.
     * <p>
     *     If the specified widget is present in this container, it is removed and detached
     *     from the UI. If the widget is not found, the method does nothing.
     * </p>
     *
     * <p>
     *     In containers where the presence of a widget is mandatory (e.g., decorators or
     *     single-slot layouts), removal of the widget will trigger automatic creation of a
     *     replacement placeholder widget.
     * </p>
     *
     * @param widget The widget to remove; if not contained, the call has no effect
     */
    void remove(Widget widget);
}
