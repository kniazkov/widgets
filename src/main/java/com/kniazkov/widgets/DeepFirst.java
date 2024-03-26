/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Algorithm for traversing the widget tree in depth.
 */
public final class DeepFirst {
    /**
     * Traverses the widget tree in depth, sequentially calling the visitor for each widget
     * until the next operation returns {@code false}. Visitors are called for the deepest widgets first,
     * and only then for the parents. The root widget is processed last.
     * @param widget Subtree root widget
     * @param visitor A widget visitor
     * @return Whether all the nodes have been visited
     */
    public static boolean traverse(final @NotNull Widget widget, final @NotNull WidgetVisitor visitor) {
        if (widget instanceof Container) {
            final Container container = (Container) widget;
            final int count = container.getChildCount();
            for (int index = 0; index < count; index++) {
                final Widget child = container.getChild(index);
                boolean result = DeepFirst.traverse(child, visitor);
                if (!result) {
                    return false;
                }
            }
        }
        return widget.accept(visitor);
    }
}
