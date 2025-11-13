/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.FontFace;
import com.kniazkov.widgets.common.FontSize;
import com.kniazkov.widgets.common.FontWeight;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that displays stylable text.
 * <p>
 * Extends {@link HasText} to support additional reactive text style attributes:
 * <ul>
 *   <li><b>Font face</b> — the typeface used to render the text ({@link FontFace})</li>
 *   <li><b>Font size</b> — the size of the text ({@link FontSize})</li>
 *   <li><b>Font weight</b> — e.g. 400, 700 ({@link FontWeight})</li>
 *   <li><b>Italic</b> — whether italics are enabled ({@code boolean})</li>
 * </ul>
 */
public interface HasStyledText extends HasText {
    /**
     * Returns the {@link Model} storing the font face for the specified {@link State}.
     *
     * @param state the logical state whose font face model is requested
     * @return a non-null model containing the font face for that state
     */
    default Model<FontFace> getFontFaceModel(final State state) {
        return this.getModel(state, Property.FONT_FACE);
    }

    /**
     * Associates a new font face model with the specified {@link State}.
     *
     * @param state the logical state to assign the model to
     * @param model the font face model (must not be {@code null})
     */
    default void setFontFaceModel(final State state, final Model<FontFace> model) {
        this.setModel(state, Property.FONT_FACE, model);
    }

    /**
     * Returns the font face value for the specified {@link State}.
     *
     * @param state the logical state whose font face is requested
     * @return the current font face value for that state
     */
    default FontFace getFontFace(final State state) {
        return this.getFontFaceModel(state).getData();
    }

    /**
     * Returns the font face value for {@link State#NORMAL}.
     *
     * @return the font face used in the normal state
     */
    default FontFace getFontFace() {
        return this.getFontFace(State.NORMAL);
    }

    /**
     * Updates the font face for the specified {@link State}.
     *
     * @param state the logical state to update
     * @param face the new font face value
     */
    default void setFontFace(final State state, final FontFace face) {
        this.getFontFaceModel(state).setData(face);
    }

    /**
     * Updates the font face for all states supported by this entity.
     *
     * @param face the new font face to assign to every supported state
     */
    default void setFontFace(final FontFace face) {
        for (final State state : this.getSupportedStates()) {
            this.setFontFace(state, face);
        }
    }

    /**
     * Returns the {@link Model} storing the font size for the specified {@link State}.
     *
     * @param state the logical state whose font size model is requested
     * @return the model associated with that state (never {@code null})
     */
    default Model<FontSize> getFontSizeModel(final State state) {
        return this.getModel(state, Property.FONT_SIZE);
    }

    /**
     * Associates a new font size model with the specified {@link State}.
     *
     * @param state the logical state to assign the model to
     * @param model the font size model (must not be {@code null})
     */
    default void setFontSizeModel(final State state, final Model<FontSize> model) {
        this.setModel(state, Property.FONT_SIZE, model);
    }

    /**
     * Returns the font size value for the specified {@link State}.
     *
     * @param state the logical state whose font size is requested
     * @return the current font size for that state
     */
    default FontSize getFontSize(final State state) {
        return this.getFontSizeModel(state).getData();
    }

    /**
     * Returns the font size for {@link State#NORMAL}.
     *
     * @return the font size in the normal state
     */
    default FontSize getFontSize() {
        return this.getFontSize(State.NORMAL);
    }

    /**
     * Updates the font size for the specified {@link State}.
     *
     * @param state the logical state to update
     * @param size the new font size value
     */
    default void setFontSize(final State state, final FontSize size) {
        this.getFontSizeModel(state).setData(size);
    }

    /**
     * Updates the font size for all supported states.
     *
     * @param size the new font size to assign to every supported state
     */
    default void setFontSize(final FontSize size) {
        for (final State state : this.getSupportedStates()) {
            this.setFontSize(state, size);
        }
    }

    /**
     * Returns the {@link Model} storing the font weight for the specified {@link State}.
     *
     * @param state the logical state whose font weight model is requested
     * @return the model associated with that state
     */
    default Model<FontWeight> getFontWeightModel(final State state) {
        return this.getModel(state, Property.FONT_WEIGHT);
    }

    /**
     * Associates a new font weight model with the specified {@link State}.
     *
     * @param state the logical state to assign the model to
     * @param model the font weight model (must not be {@code null})
     */
    default void setFontWeightModel(final State state, final Model<FontWeight> model) {
        this.setModel(state, Property.FONT_WEIGHT, model);
    }

    /**
     * Returns the font weight value for the specified {@link State}.
     *
     * @param state the logical state whose font weight is requested
     * @return the current font weight for that state
     */
    default FontWeight getFontWeight(final State state) {
        return this.getFontWeightModel(state).getData();
    }

    /**
     * Returns the font weight for {@link State#NORMAL}.
     *
     * @return the font weight in the normal state
     */
    default FontWeight getFontWeight() {
        return this.getFontWeight(State.NORMAL);
    }

    /**
     * Updates the font weight for the specified {@link State}.
     *
     * @param state the logical state to update
     * @param weight the new font weight value
     */
    default void setFontWeight(final State state, final FontWeight weight) {
        this.getFontWeightModel(state).setData(weight);
    }

    /**
     * Updates the font weight for all supported states.
     *
     * @param weight the new font weight to assign to every supported state
     */
    default void setFontWeight(final FontWeight weight) {
        for (final State state : this.getSupportedStates()) {
            this.setFontWeight(state, weight);
        }
    }

    /**
     * Returns the {@link Model} storing the italic flag for the specified {@link State}.
     *
     * @param state the logical state whose italic model is requested
     * @return a model containing a boolean flag indicating italic state
     */
    default Model<Boolean> getItalicModel(final State state) {
        return this.getModel(state, Property.ITALIC);
    }

    /**
     * Associates a new italic model with the specified {@link State}.
     *
     * @param state the logical state to assign the model to
     * @param model the italic model (must not be {@code null})
     */
    default void setItalicModel(final State state, final Model<Boolean> model) {
        this.setModel(state, Property.ITALIC, model);
    }

    /**
     * Returns whether italics are enabled for the specified {@link State}.
     *
     * @param state the logical state to query
     * @return {@code true} if italic style is enabled, otherwise {@code false}
     */
    default boolean isItalic(final State state) {
        return this.getItalicModel(state).getData();
    }

    /**
     * Returns the italic flag for {@link State#NORMAL}.
     *
     * @return {@code true} if italic style is enabled in the normal state
     */
    default boolean isItalic() {
        return this.isItalic(State.NORMAL);
    }

    /**
     * Updates the italic flag for the specified {@link State}.
     *
     * @param state the logical state to update
     * @param italic {@code true} to enable italics, {@code false} to disable
     */
    default void setItalic(final State state, final boolean italic) {
        this.getItalicModel(state).setData(italic);
    }

    /**
     * Updates the italic flag for all supported states.
     *
     * @param italic the new italic flag to assign to every supported state
     */
    default void setItalic(final boolean italic) {
        for (final State state : this.getSupportedStates()) {
            this.setItalic(state, italic);
        }
    }
}
