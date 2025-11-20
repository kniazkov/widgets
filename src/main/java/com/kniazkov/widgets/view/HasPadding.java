/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.Offset;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has an associated padding, represented as an {@link Offset}.
 */
public interface HasPadding extends Entity {

    /**
     * Returns the model that stores the padding for this view.
     *
     * @return the padding model
     */
    default Model<Offset> getPaddingModel() {
        return this.getModel(State.ANY, Property.PADDING);
    }

    /**
     * Sets a new model that stores the padding for this view.
     *
     * @param model the padding model to set
     */
    default void setPaddingModel(final Model<Offset> model) {
        this.setModel(State.ANY, Property.PADDING, model);
    }

    /**
     * Returns the current padding from the model.
     *
     * @return the current padding
     */
    default Offset getPadding() {
        return this.getPaddingModel().getData();
    }

    /**
     * Returns the left padding offset.
     *
     * @return the left padding value
     */
    default AbsoluteSize getPaddingLeft() {
        return this.getPadding().getLeft();
    }

    /**
     * Returns the right padding offset.
     *
     * @return the right padding value
     */
    default AbsoluteSize getPaddingRight() {
        return this.getPadding().getRight();
    }

    /**
     * Returns the top padding offset.
     *
     * @return the top padding value
     */
    default AbsoluteSize getPaddingTop() {
        return this.getPadding().getTop();
    }

    /**
     * Returns the bottom padding offset.
     *
     * @return the bottom padding value
     */
    default AbsoluteSize getPaddingBottom() {
        return this.getPadding().getBottom();
    }

    /**
     * Updates the padding value in the associated model.
     *
     * @param padding the new padding
     */
    default void setPadding(final Offset padding) {
        this.getPaddingModel().setData(padding);
    }

    /**
     * Updates the padding with a uniform offset for all sides.
     *
     * @param size the size applied to all sides
     */
    default void setPadding(final AbsoluteSize size) {
        this.setPadding(new Offset(size));
    }

    /**
     * Updates the padding with a uniform string-based size value for all sides.
     *
     * @param size the string representation of the size to apply to all sides
     */
    default void setPadding(final String size) {
        this.setPadding(new Offset(size));
    }

    /**
     * Updates the padding with a uniform pixel-based size for all sides.
     *
     * @param px the padding in pixels (must be ≥ 0)
     */
    default void setPadding(final int px) {
        this.setPadding(new Offset(px));
    }

    /**
     * Updates the padding with horizontal and vertical sizes.
     *
     * @param horizontal the horizontal padding (left and right)
     * @param vertical   the vertical padding (top and bottom)
     */
    default void setPadding(final AbsoluteSize horizontal, final AbsoluteSize vertical) {
        this.setPadding(new Offset(horizontal, vertical));
    }

    /**
     * Updates the padding with horizontal and vertical string-based sizes.
     *
     * @param horizontal the string representing the horizontal padding (left and right)
     * @param vertical   the string representing the vertical padding (top and bottom)
     */
    default void setPadding(final String horizontal, final String vertical) {
        this.setPadding(new Offset(horizontal, vertical));
    }

    /**
     * Updates the padding with horizontal and vertical pixel-based sizes.
     *
     * @param horizontal the horizontal padding in pixels (left and right), must be ≥ 0
     * @param vertical   the vertical padding in pixels (top and bottom), must be ≥ 0
     */
    default void setPadding(final int horizontal, final int vertical) {
        this.setPadding(new Offset(horizontal, vertical));
    }

    /**
     * Updates the padding with explicit sizes for all four sides.
     *
     * @param left   the left padding
     * @param right  the right padding
     * @param top    the top padding
     * @param bottom the bottom padding
     */
    default void setPadding(final AbsoluteSize left, final AbsoluteSize right,
            final AbsoluteSize top, final AbsoluteSize bottom) {
        this.setPadding(new Offset(left, right, top, bottom));
    }

    /**
     * Updates the padding with explicit string-based sizes for all four sides.
     *
     * @param left   the string representing the left padding
     * @param right  the string representing the right padding
     * @param top    the string representing the top padding
     * @param bottom the string representing the bottom padding
     */
    default void setPadding(final String left, final String right,
            final String top, final String bottom) {
        this.setPadding(new Offset(left, right, top, bottom));
    }

    /**
     * Updates the padding with explicit pixel-based values for all four sides.
     *
     * @param left   the left padding in pixels (must be ≥ 0)
     * @param right  the right padding in pixels (must be ≥ 0)
     * @param top    the top padding in pixels (must be ≥ 0)
     * @param bottom the bottom padding in pixels (must be ≥ 0)
     */
    default void setPadding(final int left, final int right,
            final int top, final int bottom) {
        this.setPadding(new Offset(left, right, top, bottom));
    }

    /**
     * Updates the left padding offset.
     *
     * @param value the new left padding
     */
    default void setLeftPadding(final AbsoluteSize value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setLeft(value));
    }

    /**
     * Updates the left padding offset using a string-based value.
     *
     * @param value the string representing the new left padding
     */
    default void setLeftPadding(final String value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setLeft(value));
    }

    /**
     * Updates the left padding offset using a pixel-based value.
     *
     * @param px the new left padding in pixels (must be ≥ 0)
     */
    default void setLeftPadding(final int px) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setLeft(px));
    }

    /**
     * Updates the right padding offset.
     *
     * @param value the new right padding
     */
    default void setRightPadding(final AbsoluteSize value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setRight(value));
    }

    /**
     * Updates the right padding offset using a string-based value.
     *
     * @param value the string representing the new right padding
     */
    default void setRightPadding(final String value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setRight(value));
    }

    /**
     * Updates the right padding offset using a pixel-based value.
     *
     * @param px the new right padding in pixels (must be ≥ 0)
     */
    default void setRightPadding(final int px) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setRight(px));
    }

    /**
     * Updates the top padding offset.
     *
     * @param value the new top padding
     */
    default void setTopPadding(final AbsoluteSize value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setTop(value));
    }

    /**
     * Updates the top padding offset using a string-based value.
     *
     * @param value the string representing the new top padding
     */
    default void setTopPadding(final String value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setTop(value));
    }

    /**
     * Updates the top padding offset using a pixel-based value.
     *
     * @param px the new top padding in pixels (must be ≥ 0)
     */
    default void setTopPadding(final int px) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setTop(px));
    }

    /**
     * Updates the bottom padding offset.
     *
     * @param value the new bottom padding
     */
    default void setBottomPadding(final AbsoluteSize value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setBottom(value));
    }

    /**
     * Updates the bottom padding offset using a string-based value.
     *
     * @param value the string representing the new bottom padding
     */
    default void setBottomPadding(final String value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setBottom(value));
    }

    /**
     * Updates the bottom padding offset using a pixel-based value.
     *
     * @param px the new bottom padding in pixels (must be ≥ 0)
     */
    default void setBottomPadding(final int px) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setBottom(px));
    }

    /**
     * Updates both left and right padding offsets with the same value.
     *
     * @param value the new horizontal padding
     */
    default void setHorizontalPadding(final AbsoluteSize value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setHorizontal(value));
    }

    /**
     * Updates both left and right padding offsets with the same string-based value.
     *
     * @param value the string representing the new horizontal padding
     */
    default void setHorizontalPadding(final String value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setHorizontal(value));
    }

    /**
     * Updates both left and right padding offsets using a pixel-based value.
     *
     * @param px the new horizontal padding in pixels (must be ≥ 0)
     */
    default void setHorizontalPadding(final int px) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setHorizontal(px));
    }

    /**
     * Updates both top and bottom padding offsets with the same value.
     *
     * @param value the new vertical padding
     */
    default void setVerticalPadding(final AbsoluteSize value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setVertical(value));
    }

    /**
     * Updates both top and bottom padding offsets with the same string-based value.
     *
     * @param value the string representing the new vertical padding
     */
    default void setVerticalPadding(final String value) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setVertical(value));
    }

    /**
     * Updates both top and bottom padding offsets using a pixel-based value.
     *
     * @param px the new vertical padding in pixels (must be ≥ 0)
     */
    default void setVerticalPadding(final int px) {
        final Model<Offset> model = this.getPaddingModel();
        model.setData(model.getData().setVertical(px));
    }
}
