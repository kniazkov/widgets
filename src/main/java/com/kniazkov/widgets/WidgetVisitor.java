/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Visitor interface for widgets.
 * <p>
 *     Allows performing operations on different widget types using the visitor pattern.
 *     Each method corresponds to a specific widget type, and by default delegates to
 *     {@link #byDefault(Widget)}.
 * </p>
 */
public interface WidgetVisitor {
    /**
     * Called for a {@link Button} widget.
     *
     * @param widget The widget to process
     * @return Result of the operation
     */
    default boolean visit(Button widget) {
        return this.byDefault(widget);
    }

    /**
     * Called for an {@link InputField} widget.
     *
     * @param widget The widget to process
     * @return Result of the operation
     */
    default boolean visit(InputField widget) {
        return this.byDefault(widget);
    }

    /**
     * Called for a {@link Paragraph} widget.
     *
     * @param widget The widget to process
     * @return Result of the operation
     */
    default boolean visit(Paragraph widget) {
        return this.byDefault(widget);
    }

    /**
     * Called for the {@link RootWidget}.
     *
     * @param widget The widget to process
     * @return Result of the operation
     */
    default boolean visit(RootWidget widget) {
        return this.byDefault(widget);
    }

    /**
     * Called for a {@link TextWidget}.
     *
     * @param widget The widget to process
     * @return Result of the operation
     */
    default boolean visit(TextWidget widget) {
        return this.byDefault(widget);
    }

    /**
     * Called when no specific method is provided for a widget type.
     *
     * @param widget The widget to process
     * @return Result of the operation
     */
    boolean byDefault(Widget widget);
}
