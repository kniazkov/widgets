/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Utility class for depth-first widget tree traversal.
 * <p>
 *     Supports both pre-order and post-order traversal strategies.
 * </p>
 */
public final class DepthFirst {

    private DepthFirst() {
        // static utility class
    }

    /**
     * Traverses the widget tree using depth-first post-order traversal.
     * Children are visited before their parent. Traversal stops if the visitor returns false.
     *
     * @param widget The root widget
     * @param visitor The visitor
     * @return {@code true} if traversal completed fully, {@code false} if stopped early
     */
    public static boolean postOrder(final Widget widget, final WidgetVisitor visitor) {
        if (widget instanceof Container) {
            for (final Widget child : ((Container) widget).getChildren()) {
                if (!postOrder(child, visitor)) return false;
            }
        }
        return widget.accept(visitor);
    }

    /**
     * Traverses the widget tree using depth-first pre-order traversal.
     * Parents are visited before their children. Traversal stops if the visitor returns false.
     *
     * @param widget The root widget
     * @param visitor The visitor
     * @return {@code true} if traversal completed fully, {@code false} if stopped early
     */
    public static boolean preOrder(final Widget widget, final WidgetVisitor visitor) {
        if (!widget.accept(visitor)) return false;
        if (widget instanceof Container) {
            for (final Widget child : ((Container) widget).getChildren()) {
                if (!preOrder(child, visitor)) return false;
            }
        }
        return true;
    }

    /**
     * Traverses the tree in post-order, visiting only widgets that match the filter.
     *
     * @param widget The root widget
     * @param visitor The visitor
     * @param filter Predicate to determine whether a widget should be visited
     * @return {@code true} if traversal completed fully, {@code false} if stopped early
     */
    public static boolean postOrder(final Widget widget, final WidgetVisitor visitor,
            final Predicate<Widget> filter) {
        if (widget instanceof Container) {
            for (final Widget child : ((Container) widget).getChildren()) {
                if (!postOrder(child, visitor, filter)) return false;
            }
        }
        return filter.test(widget) && widget.accept(visitor);
    }

    /**
     * Traverses the tree in pre-order, visiting only widgets that match the filter.
     *
     * @param widget The root widget
     * @param visitor The visitor
     * @param filter Predicate to determine whether a widget should be visited
     * @return {@code true} if traversal completed fully, {@code false} if stopped early
     */
    public static boolean preOrder(final Widget widget, final WidgetVisitor visitor,
            final Predicate<Widget> filter) {
        if (!filter.test(widget)) return true;
        if (!widget.accept(visitor)) return false;
        if (widget instanceof Container) {
            for (final Widget child : ((Container) widget).getChildren()) {
                if (!preOrder(child, visitor, filter)) return false;
            }
        }
        return true;
    }
}
