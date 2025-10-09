/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.UId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Represents a logical container of {@link Widget}s.
 * A {@code Container} is responsible for holding and managing child widgets. Unlike {@link Widget},
 * a container DOES NOT HAVE to be part of the visual hierarchy itself — it is simply an object
 * that owns or groups widgets.
 */
public interface Container extends Iterable<Widget> {
    /**
     * Returns the unique identifier of this container.
     *
     * @return the container ID
     */
    UId getId();

    /**
     * Returns the number of widgets currently stored in this container.
     *
     * @return the number of child widgets
     */
    int getChildCount();

    /**
     * Returns the child widget at the specified index.
     *
     * @param index the index of the child (from {@code 0} to {@code getChildCount() - 1})
     * @return the widget at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    Widget getChild(int index) throws IndexOutOfBoundsException;

    /**
     * Removes a widget from this container.
     * The exact behavior depends on the container implementation: some containers may simply
     * remove the widget; others may replace it with a default widget if empty state is not allowed;
     * as a result, the total number of widgets MAY remain unchanged.
     *
     * @param widget the widget to remove
     */
    void remove(Widget widget);

    @Override
    default Iterator<Widget> iterator() {
        return new WidgetIterator(this);
    }

    /**
     * An iterator that performs a depth-first traversal over all widgets contained in a
     * {@link Container}. The traversal starts with the container itself (if it is a {@link Widget})
     * and continues through all its descendants using an explicit stack.
     */
    class WidgetIterator implements Iterator<Widget> {

        private final Deque<Widget> stack = new ArrayDeque<>();

        /**
         * Creates an iterator that traverses widgets in the given container.
         *
         * @param root the root container to start traversal from
         */
        public WidgetIterator(final Container root) {
            if (root instanceof Widget) {
                this.stack.push((Widget) root);
            } else {
                for (int index = root.getChildCount() - 1; index >= 0; index--) {
                    this.stack.push(root.getChild(index));
                }
            }
        }

        @Override
        public boolean hasNext() {
            return !this.stack.isEmpty();
        }

        @Override
        public Widget next() {
            if (this.stack.isEmpty()) {
                throw new NoSuchElementException();
            }

            Widget widget = this.stack.pop();

            if (widget instanceof Container) {
                Container container = (Container) widget;
                for (int i = container.getChildCount() - 1; i >= 0; i--) {
                    this.stack.push(container.getChild(i));
                }
            }

            return widget;
        }
    }
}
