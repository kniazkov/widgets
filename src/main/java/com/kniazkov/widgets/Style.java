/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Represents a reusable style that can be applied to widgets.
 * <p>
 *     A style defines how a widget should appear (e.g., font, color, size).
 *     Styles can be applied to multiple widgets, and shared across them.
 * </p>
 *
 * @param <T> the type of widget this style applies to
 */
public interface Style<T extends Widget> {

    /**
     * Applies this style to the given widget.
     * <p>
     *     During this operation, each model stored in the style (such as font face, color, etc.)
     *     is forked using {@link Model#fork()}, and the resulting forked model is assigned
     *     to the widget.
     * </p>
     *
     * <p>
     *     This allows all widgets styled with the same {@code Style} instance to remain
     *     synchronized: when the style's original model changes, all forked models
     *     (in widgets) inherit those changes.
     * </p>
     *
     * <p>
     *     However, if a widget later modifies its forked model directly
     *     (e.g., via {@code setData()}), it detaches from the prototype — and stops reacting to
     *     further changes in the shared style. This ensures that:
     * </p>
     * <ul>
     *     <li>Widgets are automatically updated when the shared style changes;</li>
     *     <li>Individual overrides on a per-widget basis are still possible.</li>
     * </ul>
     *
     * @param widget The widget to apply this style to
     */
    void apply(T widget);

    /**
     * Creates a new style based on this one, with forked models.
     * <p>
     *     Each model inside the style (e.g., font, color) is duplicated using its
     *     {@link Model#fork()} method. This means the returned style will inherit data from
     *     this one, and will stay synchronized until its models are modified — at which point
     *     they become independent.
     * </p>
     *
     * @return A new style with forked (prototype-linked) models
     */
    Style<T> fork();
}
