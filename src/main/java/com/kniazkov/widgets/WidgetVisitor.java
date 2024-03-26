/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

/**
 * Visitor interface for widgets. Allows to define some operation with widgets.
 */
public interface WidgetVisitor {
    /**
     * Performs some operation with a widget of {@link Button} type.
     * @param widget A widget
     * @return Result of the operation
     */
    default boolean visit(@NotNull Button widget) {
        return this.byDefault(widget);
    }

    /**
     * Performs some operation with a widget of {@link InputField} type.
     * @param widget A widget
     * @return Result of the operation
     */
    default boolean visit(@NotNull InputField widget) {
        return this.byDefault(widget);
    }

    /**
     * Performs some operation with a widget of {@link RootWidget} type.
     * @param widget A widget
     * @return Result of the operation
     */
    default boolean visit(@NotNull RootWidget widget) {
        return this.byDefault(widget);
    }

    /**
     * Performs some operation with a widget of {@link TextWidget} type.
     * @param widget A widget
     * @return Result of the operation
     */
    default boolean visit(@NotNull TextWidget widget) {
        return this.byDefault(widget);
    }

    /**
     * Performs some default operation with a widget if no operation has been defined for this widget type.
     * @param widget A widget
     * @return Result of the operation
     */
    boolean byDefault(@NotNull Widget widget);
}
