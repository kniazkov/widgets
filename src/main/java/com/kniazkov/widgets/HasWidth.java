/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * An entity that has a configurable width value.
 * <p>
 *     This interface defines a contract for UI components that support a width property,
 *     which is typically expressed as a {@link WidgetSize} and managed through a {@link Model}.
 *     The model enables reactive updates, data binding, and prototype-based inheritance.
 * </p>
 *
 * @param <T> The specific type of {@link WidgetSize} used for this component
 *               (e.g., {@code InlineWidgetSize})
 */
public interface HasWidth<T extends WidgetSize> {

    /**
     * Returns the model that manages the width value of this entity.
     *
     * @return Model representing the width
     */
    Model<T> getWidthModel();

    /**
     * Replaces the width model associated with this entity.
     *
     * @param model New model to manage width
     */
    void setWidthModel(Model<T> model);

    /**
     * Retrieves the current width value.
     * <p>
     *     If not explicitly set, the value may be inherited from a prototype model
     *     or default to an undefined state.
     * </p>
     *
     * @return Current width
     */
    default WidgetSize getWidth() {
        return this.getWidthModel().getData();
    }

    /**
     * Updates the width value directly in the model.
     *
     * @param width New width to apply
     */
    default void setWidth(T width) {
        this.getWidthModel().setData(width);
    }
}
