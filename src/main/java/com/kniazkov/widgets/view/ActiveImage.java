/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.controller.HandlesPointerEvents;
import com.kniazkov.widgets.images.ImageSource;
import com.kniazkov.widgets.model.ImageSourceModel;
import com.kniazkov.widgets.model.Model;
import java.util.Set;

/**
 * Widget representing an image that can respond to user interaction by changing its visual state.
 * Supports three states: normal, hovered (mouse over), and active (pressed/clicked).
 * All states can share the same image source or have individual ones.
 */
public class ActiveImage extends BaseImageWidget implements HandlesPointerEvents {
    /**
     * Set of supported states.
     */
    private static final Set<State> SUPPORTED_STATES = State.setOf(
        State.NORMAL,
        State.HOVERED,
        State.ACTIVE
    );

    /**
     * Creates an ActiveImage widget with the specified image source.
     * The same source will be used for all interaction states.
     *
     * @param source the {@link ImageSource} to display
     */
    public ActiveImage(final ImageSource source) {
        final Model<ImageSource> model = new ImageSourceModel(source);
        this.setSourceModel(State.NORMAL, model);
        this.setSourceModel(State.HOVERED, model.asCascading());
        this.setSourceModel(State.ACTIVE, model.asCascading());
    }

    /**
     * Creates an ActiveImage widget with a hyperlink as the image source.
     * The same hyperlink will be used for all interaction states.
     *
     * @param href the raw hyperlink string pointing to the image
     */
    public ActiveImage(final String href) {
        this(ImageSource.fromHyperlink(href));
    }

    @Override
    public String getType() {
        return "active image";
    }

    @Override
    public Set<State> getSupportedStates() {
        return SUPPORTED_STATES;
    }

    /**
     * Returns the image source model associated with a specific widget state.
     *
     * @param state the widget state (normal, hovered, or active)
     * @return the {@link Model} containing the {@link ImageSource} for the given state
     */
    public Model<ImageSource> getSourceModel(final State state) {
        return this.getModel(state, Property.IMAGE_SOURCE);
    }

    /**
     * Sets the image source model for a specific widget state.
     *
     * @param state the widget state (normal, hovered, or active)
     * @param model the {@link Model} containing the {@link ImageSource} to use for this state
     */
    public void setSourceModel(final State state, final Model<ImageSource> model) {
        this.setModel(state, Property.IMAGE_SOURCE, model);
    }

    /**
     * Gets the image source for a specific widget state.
     *
     * @param state the widget state (normal, hovered, or active)
     * @return the {@link ImageSource} currently used for the given state
     */
    public ImageSource getSource(final State state) {
        return this.getSourceModel(state).getData();
    }

    /**
     * Gets the image source used in the normal (default) state.
     * This is a convenience method equivalent to {@code getSource(State.NORMAL)}.
     *
     * @return the {@link ImageSource} for the normal state
     */
    public ImageSource getSource() {
        return this.getSourceModel(State.NORMAL).getData();
    }

    /**
     * Sets the image source for a specific widget state.
     *
     * @param state  the widget state (normal, hovered, or active)
     * @param source the {@link ImageSource} to use for this state
     */
    public void setSource(final State state, final ImageSource source) {
        this.getSourceModel(state).setData(source);
    }

    /**
     * Sets the same image source for all supported widget states (normal, hovered, active).
     *
     * @param source the {@link ImageSource} to use for all states
     */
    public void setSource(final ImageSource source) {
        for (final State state : this.getSupportedStates()) {
            this.setSource(state, source);
        }
    }

    /**
     * Sets a hyperlink as the image source for all supported widget states.
     *
     * @param href the raw hyperlink string pointing to the image
     */
    public void setSource(final String href) {
        this.setSource(ImageSource.fromHyperlink(href));
    }
}
