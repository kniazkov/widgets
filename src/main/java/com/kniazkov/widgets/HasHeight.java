/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * An entity that has a configurable height value.
 * <p>
 *     This interface defines a contract for UI components that support a height property,
 *     typically represented by a {@link WidgetSize}. The height is managed through a {@link Model},
 *     supporting data binding and prototype-based inheritance.
 * </p>
 *
 * @param <T> The specific type of {@link WidgetSize} used for this component
 *               (e.g., {@code InlineWidgetSize})
 */
public interface HasHeight<T extends WidgetSize> {

    /**
     * Returns the model that manages the height value of this entity.
     *
     * @return Model representing the height
     */
    Model<T> getHeightModel();

    /**
     * Replaces the height model associated with this entity.
     *
     * @param model New model to manage height
     */
    void setHeightModel(Model<T> model);

    /**
     * Retrieves the current height value.
     * <p>
     *     If not explicitly set, the value may be inherited or remain undefined.
     * </p>
     *
     * @return Current height
     */
    default WidgetSize getHeight() {
        return this.getHeightModel().getData();
    }

    /**
     * Updates the height value directly in the model.
     *
     * @param height New height to apply
     */
    default void setHeight(T height) {
        this.getHeightModel().setData(height);
    }
}
