/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.Offset;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has an associated absolute position, represented as an {@link Offset}.
 */
public interface HasAbsolutePosition extends Entity {

    /**
     * Returns the model that stores the absolute position for this view.
     *
     * @return the absolute position model
     */
    default Model<Offset> getAbsolutePositionModel() {
        return this.getModel(State.ANY, Property.ABSOLUTE_POSITION);
    }

    /**
     * Sets a new model that stores the absolute position for this view.
     *
     * @param model the absolute position model to set
     */
    default void setAbsolutePositionModel(final Model<Offset> model) {
        this.setModel(State.ANY, Property.ABSOLUTE_POSITION, model);
    }

    /**
     * Returns the current absolute position from the model.
     *
     * @return the current absolute position
     */
    default Offset getAbsolutePosition() {
        return this.getAbsolutePositionModel().getData();
    }

    /**
     * Returns the left position offset.
     *
     * @return the left position value
     */
    default AbsoluteSize getLeftPosition() {
        return this.getAbsolutePosition().getLeft();
    }

    /**
     * Returns the right position offset.
     *
     * @return the right position value
     */
    default AbsoluteSize getRightPosition() {
        return this.getAbsolutePosition().getRight();
    }

    /**
     * Returns the top position offset.
     *
     * @return the top position value
     */
    default AbsoluteSize getTopPosition() {
        return this.getAbsolutePosition().getTop();
    }

    /**
     * Returns the bottom position offset.
     *
     * @return the bottom position value
     */
    default AbsoluteSize getBottomPosition() {
        return this.getAbsolutePosition().getBottom();
    }

    /**
     * Updates the absolute position value in the associated model.
     *
     * @param position the new absolute position
     */
    default void setAbsolutePosition(final Offset position) {
        this.getAbsolutePositionModel().setData(position);
    }

    /**
     * Updates the absolute position with a uniform offset for all sides.
     *
     * @param size the size applied to all sides
     */
    default void setAbsolutePosition(final AbsoluteSize size) {
        this.setAbsolutePosition(new Offset(size));
    }

    /**
     * Updates the absolute position with a uniform string-based size value for all sides.
     *
     * @param size the string representation of the size to apply to all sides
     */
    default void setAbsolutePosition(final String size) {
        this.setAbsolutePosition(new Offset(size));
    }

    /**
     * Updates the absolute position with a uniform pixel-based size value for all sides.
     *
     * @param px the position in pixels (must be ≥ 0)
     */
    default void setAbsolutePosition(final int px) {
        this.setAbsolutePosition(new Offset(px));
    }

    /**
     * Updates the absolute position with horizontal and vertical sizes.
     *
     * @param horizontal the horizontal position (left and right)
     * @param vertical   the vertical position (top and bottom)
     */
    default void setAbsolutePosition(final AbsoluteSize horizontal, final AbsoluteSize vertical) {
        this.setAbsolutePosition(new Offset(horizontal, vertical));
    }

    /**
     * Updates the absolute position with horizontal and vertical string-based sizes.
     *
     * @param horizontal the string representing the horizontal position (left and right)
     * @param vertical   the string representing the vertical position (top and bottom)
     */
    default void setAbsolutePosition(final String horizontal, final String vertical) {
        this.setAbsolutePosition(new Offset(horizontal, vertical));
    }

    /**
     * Updates the absolute position with horizontal and vertical pixel-based sizes.
     *
     * @param horizontal the horizontal position (left and right), in pixels (must be ≥ 0)
     * @param vertical   the vertical position (top and bottom), in pixels (must be ≥ 0)
     */
    default void setAbsolutePosition(final int horizontal, final int vertical) {
        this.setAbsolutePosition(new Offset(horizontal, vertical));
    }

    /**
     * Updates the absolute position with explicit sizes for all four sides.
     *
     * @param left   the left position
     * @param right  the right position
     * @param top    the top position
     * @param bottom the bottom position
     */
    default void setAbsolutePosition(final AbsoluteSize left, final AbsoluteSize right,
            final AbsoluteSize top, final AbsoluteSize bottom) {
        this.setAbsolutePosition(new Offset(left, right, top, bottom));
    }

    /**
     * Updates the absolute position with explicit string-based sizes for all four sides.
     *
     * @param left   the string representing the left position
     * @param right  the string representing the right position
     * @param top    the string representing the top position
     * @param bottom the string representing the bottom position
     */
    default void setAbsolutePosition(final String left, final String right,
            final String top, final String bottom) {
        this.setAbsolutePosition(new Offset(left, right, top, bottom));
    }

    /**
     * Updates the absolute position with explicit pixel-based sizes for all four sides.
     *
     * @param left   the left position in pixels (must be ≥ 0)
     * @param right  the right position in pixels (must be ≥ 0)
     * @param top    the top position in pixels (must be ≥ 0)
     * @param bottom the bottom position in pixels (must be ≥ 0)
     */
    default void setAbsolutePosition(final int left, final int right,
            final int top, final int bottom) {
        this.setAbsolutePosition(new Offset(left, right, top, bottom));
    }

    /**
     * Updates the left position offset.
     *
     * @param value the new left position
     */
    default void setLeftPosition(final AbsoluteSize value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setLeft(value));
    }

    /**
     * Updates the left position offset using a string-based value.
     *
     * @param value the string representing the new left position
     */
    default void setLeftPosition(final String value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setLeft(value));
    }

    /**
     * Updates the left position offset using a pixel-based value.
     *
     * @param px the new left position in pixels (must be ≥ 0)
     */
    default void setLeftPosition(final int px) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setLeft(px));
    }

    /**
     * Updates the right position offset.
     *
     * @param value the new right position
     */
    default void setRightPosition(final AbsoluteSize value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setRight(value));
    }

    /**
     * Updates the right position offset using a string-based value.
     *
     * @param value the string representing the new right position
     */
    default void setRightPosition(final String value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setRight(value));
    }

    /**
     * Updates the right position offset using a pixel-based value.
     *
     * @param px the new right position in pixels (must be ≥ 0)
     */
    default void setRightPosition(final int px) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setRight(px));
    }

    /**
     * Updates the top position offset.
     *
     * @param value the new top position
     */
    default void setTopPosition(final AbsoluteSize value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setTop(value));
    }

    /**
     * Updates the top position offset using a string-based value.
     *
     * @param value the string representing the new top position
     */
    default void setTopPosition(final String value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setTop(value));
    }

    /**
     * Updates the top position offset using a pixel-based value.
     *
     * @param px the new top position in pixels (must be ≥ 0)
     */
    default void setTopPosition(final int px) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setTop(px));
    }

    /**
     * Updates the bottom position offset.
     *
     * @param value the new bottom position
     */
    default void setBottomPosition(final AbsoluteSize value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setBottom(value));
    }

    /**
     * Updates the bottom position offset using a string-based value.
     *
     * @param value the string representing the new bottom position
     */
    default void setBottomPosition(final String value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setBottom(value));
    }

    /**
     * Updates the bottom position offset using a pixel-based value.
     *
     * @param px the new bottom position in pixels (must be ≥ 0)
     */
    default void setBottomPosition(final int px) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setBottom(px));
    }

    /**
     * Updates both left and right position offsets with the same value.
     *
     * @param value the new horizontal position
     */
    default void setHorizontalPosition(final AbsoluteSize value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setHorizontal(value));
    }

    /**
     * Updates both left and right position offsets with the same string-based value.
     *
     * @param value the string representing the new horizontal position
     */
    default void setHorizontalPosition(final String value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setHorizontal(value));
    }

    /**
     * Updates both left and right position offsets using a pixel-based value.
     *
     * @param px the new horizontal position in pixels (must be ≥ 0)
     */
    default void setHorizontalPosition(final int px) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setHorizontal(px));
    }

    /**
     * Updates both top and bottom position offsets with the same value.
     *
     * @param value the new vertical position
     */
    default void setVerticalPosition(final AbsoluteSize value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setVertical(value));
    }

    /**
     * Updates both top and bottom position offsets with the same string-based value.
     *
     * @param value the string representing the new vertical position
     */
    default void setVerticalPosition(final String value) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setVertical(value));
    }

    /**
     * Updates both top and bottom position offsets using a pixel-based value.
     *
     * @param px the new vertical position in pixels (must be ≥ 0)
     */
    default void setVerticalPosition(final int px) {
        final Model<Offset> model = this.getAbsolutePositionModel();
        model.setData(model.getData().setVertical(px));
    }
}
