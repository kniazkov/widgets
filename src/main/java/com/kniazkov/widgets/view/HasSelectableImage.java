/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.images.SvgImageSource;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has image models for selected and unselected states.
 */
public interface HasSelectableImage extends Entity {
    /**
     * Returns the model that stores the image for the selected state.
     *
     * @return the selected-state image model
     */
    default Model<SvgImageSource> getSelectedImageSourceModel() {
        return this.getModel(State.ANY, Property.SELECTED_IMAGE_SOURCE);
    }

    /**
     * Sets a new model for the selected-state image.
     *
     * @param model the model to set
     */
    default void setSelectedImageSourceModel(final Model<SvgImageSource> model) {
        this.setModel(State.ANY, Property.SELECTED_IMAGE_SOURCE, model);
    }

    /**
     * Returns the current image source for the selected state.
     *
     * @return the selected-state image source
     */
    default SvgImageSource getSelectedImageSource() {
        return this.getSelectedImageSourceModel().getData();
    }

    /**
     * Updates the image source for the selected state.
     *
     * @param source the new selected-state image source
     */
    default void setSelectedImageSource(final SvgImageSource source) {
        this.getSelectedImageSourceModel().setData(source);
    }

    /**
     * Returns the model that stores the image for the unselected state.
     *
     * @return the unselected-state image model
     */
    default Model<SvgImageSource> getUnselectedImageSourceModel() {
        return this.getModel(State.ANY, Property.UNSELECTED_IMAGE_SOURCE);
    }

    /**
     * Sets a new model for the unselected-state image.
     *
     * @param model the model to set
     */
    default void setUnselectedImageSourceModel(final Model<SvgImageSource> model) {
        this.setModel(State.ANY, Property.UNSELECTED_IMAGE_SOURCE, model);
    }

    /**
     * Returns the current image source for the unselected state.
     *
     * @return the unselected-state image source
     */
    default SvgImageSource getUnselectedImageSource() {
        return this.getUnselectedImageSourceModel().getData();
    }

    /**
     * Updates the image source for the unselected state.
     *
     * @param source the new unselected-state image source
     */
    default void setUnselectedImageSource(final SvgImageSource source) {
        this.getUnselectedImageSourceModel().setData(source);
    }
}
