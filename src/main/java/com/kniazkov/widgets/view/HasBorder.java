/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.BorderStyle;
import com.kniazkov.widgets.common.Color;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that defines visual border properties, including
 * color, style, width, and radius (corner rounding).
 */
public interface HasBorder extends Entity {
    /**
     * Returns the {@link Model} instance that stores the border color for the specified
     * {@link State}.
     *
     * @param state the logical state whose border color model is requested
     * @return a non-null border color model associated with the given state
     */
    default Model<Color> getBorderColorModel(final State state) {
        return this.getModel(state, Property.BORDER_COLOR);
    }

    /**
     * Associates a new border color model with the specified {@link State}.
     *
     * @param state the logical state to which the model should be assigned
     * @param model the border color model to associate (must not be {@code null})
     */
    default void setBorderColorModel(final State state, final Model<Color> model) {
        this.setModel(state, Property.BORDER_COLOR, model);
    }

    /**
     * Returns the current border color for the specified {@link State}.
     *
     * @param state the logical state whose border color value is requested
     * @return the current border color from the corresponding model
     */
    default Color getBorderColor(final State state) {
        return this.getBorderColorModel(state).getData();
    }

    /**
     * Returns the border color associated with the default {@link State#NORMAL}.
     *
     * @return the border color for the normal state
     */
    default Color getBorderColor() {
        return this.getBorderColor(State.NORMAL);
    }

    /**
     * Updates the border color in the model associated with the specified {@link State}.
     *
     * @param state the logical state to update
     * @param color the new border color to set
     */
    default void setBorderColor(final State state, final Color color) {
        this.getBorderColorModel(state).setData(color);
    }

    /**
     * Updates the border color for all {@link State}s supported by this entity.
     *
     * @param color the new border color to set for every supported state
     */
    default void setBorderColor(final Color color) {
        for (final State state : this.getSupportedStates()) {
            this.setBorderColor(state, color);
        }
    }

    /**
     * Returns the {@link Model} instance that stores the border style
     * for the specified {@link State}.
     *
     * @param state the logical state whose border style model is requested
     * @return a non-null border style model associated with the given state
     */
    default Model<BorderStyle> getBorderStyleModel(final State state) {
        return this.getModel(state, Property.BORDER_STYLE);
    }

    /**
     * Associates a new border style model with the specified {@link State}.
     *
     * @param state the logical state to which the model should be assigned
     * @param model the border style model to associate (must not be {@code null})
     */
    default void setBorderStyleModel(final State state, final Model<BorderStyle> model) {
        this.setModel(state, Property.BORDER_STYLE, model);
    }

    /**
     * Returns the current border style for the specified {@link State}.
     *
     * @param state the logical state whose border style value is requested
     * @return the current border style from the corresponding model
     */
    default BorderStyle getBorderStyle(final State state) {
        return this.getBorderStyleModel(state).getData();
    }

    /**
     * Returns the border style associated with the default {@link State#NORMAL}.
     *
     * @return the border style for the normal state
     */
    default BorderStyle getBorderStyle() {
        return this.getBorderStyle(State.NORMAL);
    }

    /**
     * Updates the border style in the model associated with the specified {@link State}.
     *
     * @param state the logical state to update
     * @param style the new border style to set
     */
    default void setBorderStyle(final State state, final BorderStyle style) {
        this.getBorderStyleModel(state).setData(style);
    }

    /**
     * Updates the border style for all {@link State}s supported by this entity.
     *
     * @param style the new border style to set for every supported state
     */
    default void setBorderStyle(final BorderStyle style) {
        for (final State state : this.getSupportedStates()) {
            this.setBorderStyle(state, style);
        }
    }

    /**
     * Returns the model that stores the border width for this view.
     *
     * @return the border width model
     */
    default Model<AbsoluteSize> getBorderWidthModel() {
        return this.getModel(State.ANY, Property.BORDER_WIDTH);
    }

    /**
     * Sets a new border width model for this view.
     *
     * @param model the border width model to set
     */
    default void setBorderWidthModel(final Model<AbsoluteSize> model) {
        this.setModel(State.ANY, Property.BORDER_WIDTH, model);
    }

    /**
     * Returns the current border width from the model.
     *
     * @return the current border width
     */
    default AbsoluteSize getBorderWidth() {
        return this.getBorderWidthModel().getData();
    }

    /**
     * Updates the border width value in the associated model.
     *
     * @param width the new border width
     */
    default void setBorderWidth(final AbsoluteSize width) {
        this.getBorderWidthModel().setData(width);
    }

    /**
     * Updates the border width value in the associated model using a string-based value.
     * <p>
     * Example: {@code setBorderWidth("2px")}
     *
     * @param width the string representing the new border width
     */
    default void setBorderWidth(final String width) {
        this.getBorderWidthModel().setData(AbsoluteSize.parse(width));
    }

    /**
     * Returns the model that stores the border radius (corner rounding) for this view.
     *
     * @return the border radius model
     */
    default Model<AbsoluteSize> getBorderRadiusModel() {
        return this.getModel(State.ANY, Property.BORDER_RADIUS);
    }

    /**
     * Sets a new border radius model for this view.
     *
     * @param model the border radius model to set
     */
    default void setBorderRadiusModel(final Model<AbsoluteSize> model) {
        this.setModel(State.ANY, Property.BORDER_RADIUS, model);
    }

    /**
     * Returns the current border radius (corner rounding) from the model.
     *
     * @return the current border radius
     */
    default AbsoluteSize getBorderRadius() {
        return this.getBorderRadiusModel().getData();
    }

    /**
     * Updates the border radius (corner rounding) value in the associated model.
     *
     * @param radius the new border radius
     */
    default void setBorderRadius(final AbsoluteSize radius) {
        this.getBorderRadiusModel().setData(radius);
    }

    /**
     * Updates the border radius (corner rounding) value using a string-based value.
     * <p>
     * Example: {@code setBorderRadius("4px")}
     *
     * @param radius the string representing the new border radius
     */
    default void setBorderRadius(final String radius) {
        this.getBorderRadiusModel().setData(AbsoluteSize.parse(radius));
    }
}
