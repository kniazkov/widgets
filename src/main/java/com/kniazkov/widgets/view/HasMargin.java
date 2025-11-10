/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.common.AbsoluteSize;
import com.kniazkov.widgets.common.Offset;
import com.kniazkov.widgets.model.Model;

/**
 * An {@link Entity} that has an associated margin, represented as an {@link Offset}.
 */
public interface HasMargin extends Entity {

    /**
     * Returns the model that stores the margin for this view.
     *
     * @return the margin model
     */
    default Model<Offset> getMarginModel() {
        return this.getModel(State.ANY, Property.MARGIN);
    }

    /**
     * Sets a new model that stores the margin for this view.
     *
     * @param model the margin model to set
     */
    default void setMarginModel(final Model<Offset> model) {
        this.setModel(State.ANY, Property.MARGIN, model);
    }

    /**
     * Returns the current margin from the model.
     *
     * @return the current margin
     */
    default Offset getMargin() {
        return this.getMarginModel().getData();
    }

    /**
     * Returns the left margin offset.
     *
     * @return the left margin value
     */
    default AbsoluteSize getMarginLeft() {
        return this.getMargin().getLeft();
    }

    /**
     * Returns the right margin offset.
     *
     * @return the right margin value
     */
    default AbsoluteSize getMarginRight() {
        return this.getMargin().getRight();
    }

    /**
     * Returns the top margin offset.
     *
     * @return the top margin value
     */
    default AbsoluteSize getMarginTop() {
        return this.getMargin().getTop();
    }

    /**
     * Returns the bottom margin offset.
     *
     * @return the bottom margin value
     */
    default AbsoluteSize getMarginBottom() {
        return this.getMargin().getBottom();
    }

    /**
     * Updates the margin value in the associated model.
     *
     * @param margin the new margin
     */
    default void setMargin(final Offset margin) {
        this.getMarginModel().setData(margin);
    }

    /**
     * Updates the margin with a uniform offset for all sides.
     *
     * @param size the size applied to all sides
     */
    default void setMargin(final AbsoluteSize size) {
        this.setMargin(new Offset(size));
    }

    /**
     * Updates the margin with a uniform string-based size value for all sides.
     *
     * @param size the string representation of the size to apply to all sides
     */
    default void setMargin(final String size) {
        this.setMargin(new Offset(size));
    }

    /**
     * Updates the margin with horizontal and vertical sizes.
     *
     * @param horizontal the horizontal margin (left and right)
     * @param vertical   the vertical margin (top and bottom)
     */
    default void setMargin(final AbsoluteSize horizontal, final AbsoluteSize vertical) {
        this.setMargin(new Offset(horizontal, vertical));
    }

    /**
     * Updates the margin with horizontal and vertical string-based sizes.
     *
     * @param horizontal the string representing the horizontal margin (left and right)
     * @param vertical   the string representing the vertical margin (top and bottom)
     */
    default void setMargin(final String horizontal, final String vertical) {
        this.setMargin(new Offset(horizontal, vertical));
    }

    /**
     * Updates the margin with explicit sizes for all four sides.
     *
     * @param left   the left margin
     * @param right  the right margin
     * @param top    the top margin
     * @param bottom the bottom margin
     */
    default void setMargin(final AbsoluteSize left, final AbsoluteSize right,
            final AbsoluteSize top, final AbsoluteSize bottom) {
        this.setMargin(new Offset(left, right, top, bottom));
    }

    /**
     * Updates the margin with explicit string-based sizes for all four sides.
     *
     * @param left   the string representing the left margin
     * @param right  the string representing the right margin
     * @param top    the string representing the top margin
     * @param bottom the string representing the bottom margin
     */
    default void setMargin(final String left, final String right,
            final String top, final String bottom) {
        this.setMargin(new Offset(left, right, top, bottom));
    }

    /**
     * Updates the left margin offset.
     *
     * @param value the new left margin
     */
    default void setLeftMargin(final AbsoluteSize value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setLeft(value));
    }

    /**
     * Updates the left margin offset using a string-based value.
     *
     * @param value the string representing the new left margin
     */
    default void setLeftMargin(final String value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setLeft(value));
    }

    /**
     * Updates the right margin offset.
     *
     * @param value the new right margin
     */
    default void setRightMargin(final AbsoluteSize value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setRight(value));
    }

    /**
     * Updates the right margin offset using a string-based value.
     *
     * @param value the string representing the new right margin
     */
    default void setRightMargin(final String value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setRight(value));
    }

    /**
     * Updates the top margin offset.
     *
     * @param value the new top margin
     */
    default void setTopMargin(final AbsoluteSize value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setTop(value));
    }

    /**
     * Updates the top margin offset using a string-based value.
     *
     * @param value the string representing the new top margin
     */
    default void setTopMargin(final String value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setTop(value));
    }

    /**
     * Updates the bottom margin offset.
     *
     * @param value the new bottom margin
     */
    default void setBottomMargin(final AbsoluteSize value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setBottom(value));
    }

    /**
     * Updates the bottom margin offset using a string-based value.
     *
     * @param value the string representing the new bottom margin
     */
    default void setBottomMargin(final String value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setBottom(value));
    }

    /**
     * Updates both left and right margin offsets with the same value.
     *
     * @param value the new horizontal margin
     */
    default void setHorizontalMargin(final AbsoluteSize value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setHorizontal(value));
    }

    /**
     * Updates both left and right margin offsets with the same string-based value.
     *
     * @param value the string representing the new horizontal margin
     */
    default void setHorizontalMargin(final String value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setHorizontal(value));
    }

    /**
     * Updates both top and bottom margin offsets with the same value.
     *
     * @param value the new vertical margin
     */
    default void setVerticalMargin(final AbsoluteSize value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setVertical(value));
    }

    /**
     * Updates both top and bottom margin offsets with the same string-based value.
     *
     * @param value the string representing the new vertical margin
     */
    default void setVerticalMargin(final String value) {
        final Model<Offset> model = this.getMarginModel();
        model.setData(model.getData().setVertical(value));
    }
}
