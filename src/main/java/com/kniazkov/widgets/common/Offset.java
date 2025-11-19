/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.util.Objects;
import com.kniazkov.json.JsonObject;

/**
 * Represents a set of absolute offsets (margins, paddings, or similar spacing values)
 * for the four sides of a rectangular area — left, right, top, and bottom.
 * Each offset is defined as an {@link AbsoluteSize}, which may be expressed in
 * fixed units such as pixels, points, centimeters, etc.
 * This class is immutable: all modification methods ({@code setLeft()}, {@code setTop()}, etc.)
 * return new {@code Offset} instances rather than modifying the current object.
 */
public class Offset {
    /**
     * Unspecified (undefined) offset.
     */
    public final static Offset UNDEFINED = new Offset(
        AbsoluteSize.UNDEFINED,
        AbsoluteSize.UNDEFINED,
        AbsoluteSize.UNDEFINED,
        AbsoluteSize.UNDEFINED
    );

    /**
     * Left offset.
     */
    private final AbsoluteSize left;

    /**
     * Right offset.
     */
    private final AbsoluteSize right;

    /**
     * Top offset.
     */
    private final AbsoluteSize top;

    /**
     * Bottom offset.
     */
    private final AbsoluteSize bottom;

    /**
     * Creates a uniform offset using the same size for all four sides.
     *
     * @param size the size applied to all sides
     */
    public Offset(final AbsoluteSize size) {
        this(size, size, size, size);
    }

    /**
     * Creates a uniform offset using the same string-based size value for all sides.
     * <p>
     * Example: {@code new Offset("10px")}
     *
     * @param size the string representation of the size to apply to all sides
     */
    public Offset(final String size) {
        this(AbsoluteSize.parse(size));
    }

    /**
     * Creates an offset with the same horizontal size (left and right)
     * and the same vertical size (top and bottom).
     *
     * @param horizontal the horizontal offset (left and right)
     * @param vertical   the vertical offset (top and bottom)
     */
    public Offset(final AbsoluteSize horizontal, final AbsoluteSize vertical) {
        this(horizontal, horizontal, vertical, vertical);
    }

    /**
     * Creates an offset with the same horizontal and vertical string-based size values.
     * <p>
     * Example: {@code new Offset("12px", "8px")}
     *
     * @param horizontal the string representing the horizontal offset (left and right)
     * @param vertical   the string representing the vertical offset (top and bottom)
     */
    public Offset(final String horizontal, final String vertical) {
        this(AbsoluteSize.parse(horizontal), AbsoluteSize.parse(vertical));
    }

    /**
     * Creates an offset with explicitly defined sizes for each side.
     *
     * @param left   the left offset
     * @param right  the right offset
     * @param top    the top offset
     * @param bottom the bottom offset
     */
    public Offset(final AbsoluteSize left, final AbsoluteSize right,
                  final AbsoluteSize top, final AbsoluteSize bottom) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }

    /**
     * Creates an offset with explicitly defined string-based sizes for each side.
     * <p>
     * Example: {@code new Offset("10px", "12px", "8px", "6px")}
     *
     * @param left   the string representing the left offset
     * @param right  the string representing the right offset
     * @param top    the string representing the top offset
     * @param bottom the string representing the bottom offset
     */
    public Offset(final String left, final String right,
                  final String top, final String bottom) {
        this(
            AbsoluteSize.parse(left),
            AbsoluteSize.parse(right),
            AbsoluteSize.parse(top),
            AbsoluteSize.parse(bottom)
        );
    }

    /**
     * Returns the left offset.
     *
     * @return the left offset value
     */
    public AbsoluteSize getLeft() {
        return this.left;
    }

    /**
     * Returns the right offset.
     *
     * @return the right offset value
     */
    public AbsoluteSize getRight() {
        return this.right;
    }

    /**
     * Returns the top offset.
     *
     * @return the top offset value
     */
    public AbsoluteSize getTop() {
        return this.top;
    }

    /**
     * Returns the bottom offset.
     *
     * @return the bottom offset value
     */
    public AbsoluteSize getBottom() {
        return this.bottom;
    }

    /**
     * Returns a new {@code Offset} instance with the specified left offset.
     *
     * @param value the new left offset
     * @return a new {@code Offset} with the updated left value
     */
    public Offset setLeft(final AbsoluteSize value) {
        return new Offset(value, this.right, this.top, this.bottom);
    }

    /**
     * Returns a new {@code Offset} instance with the specified right offset.
     *
     * @param value the new right offset
     * @return a new {@code Offset} with the updated right value
     */
    public Offset setRight(final AbsoluteSize value) {
        return new Offset(this.left, value, this.top, this.bottom);
    }

    /**
     * Returns a new {@code Offset} instance with the specified top offset.
     *
     * @param value the new top offset
     * @return a new {@code Offset} with the updated top value
     */
    public Offset setTop(final AbsoluteSize value) {
        return new Offset(this.left, this.right, value, this.bottom);
    }

    /**
     * Returns a new {@code Offset} instance with the specified bottom offset.
     *
     * @param value the new bottom offset
     * @return a new {@code Offset} with the updated bottom value
     */
    public Offset setBottom(final AbsoluteSize value) {
        return new Offset(this.left, this.right, this.top, value);
    }

    /**
     * Returns a new {@code Offset} instance with updated horizontal offsets
     * (left and right) set to the given value.
     *
     * @param value the new horizontal offset
     * @return a new {@code Offset} with updated horizontal offsets
     */
    public Offset setHorizontal(final AbsoluteSize value) {
        return new Offset(value, value, this.top, this.bottom);
    }

    /**
     * Returns a new {@code Offset} instance with updated vertical offsets
     * (top and bottom) set to the given value.
     *
     * @param value the new vertical offset
     * @return a new {@code Offset} with updated vertical offsets
     */
    public Offset setVertical(final AbsoluteSize value) {
        return new Offset(this.left, this.right, value, value);
    }

    /**
     * Returns a new {@code Offset} instance with the specified left offset
     * provided as a string value.
     *
     * @param value the string representing the new left offset
     * @return a new {@code Offset} with the updated left value
     */
    public Offset setLeft(final String value) {
        return new Offset(AbsoluteSize.parse(value), this.right, this.top, this.bottom);
    }

    /**
     * Returns a new {@code Offset} instance with the specified right offset
     * provided as a string value.
     *
     * @param value the string representing the new right offset
     * @return a new {@code Offset} with the updated right value
     */
    public Offset setRight(final String value) {
        return new Offset(this.left, AbsoluteSize.parse(value), this.top, this.bottom);
    }

    /**
     * Returns a new {@code Offset} instance with the specified top offset
     * provided as a string value.
     *
     * @param value the string representing the new top offset
     * @return a new {@code Offset} with the updated top value
     */
    public Offset setTop(final String value) {
        return new Offset(this.left, this.right, AbsoluteSize.parse(value), this.bottom);
    }

    /**
     * Returns a new {@code Offset} instance with the specified bottom offset
     * provided as a string value.
     *
     * @param value the string representing the new bottom offset
     * @return a new {@code Offset} with the updated bottom value
     */
    public Offset setBottom(final String value) {
        return new Offset(this.left, this.right, this.top, AbsoluteSize.parse(value));
    }

    /**
     * Returns a new {@code Offset} instance with updated horizontal offsets
     * (left and right) set to the given string-based value.
     *
     * @param value the string representing the new horizontal offset
     * @return a new {@code Offset} with updated horizontal offsets
     */
    public Offset setHorizontal(final String value) {
        AbsoluteSize size = AbsoluteSize.parse(value);
        return new Offset(size, size, this.top, this.bottom);
    }

    /**
     * Returns a new {@code Offset} instance with updated vertical offsets
     * (top and bottom) set to the given string-based value.
     *
     * @param value the string representing the new vertical offset
     * @return a new {@code Offset} with updated vertical offsets
     */
    public Offset setVertical(final String value) {
        AbsoluteSize size = AbsoluteSize.parse(value);
        return new Offset(this.left, this.right, size, size);
    }

    /**
     * Converts this offset to a JSON representation.
     *
     * @return a {@link JsonObject} representing this offset
     */
    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addString("left", this.left.getCSSCode());
        obj.addString("right", this.right.getCSSCode());
        obj.addString("top", this.top.getCSSCode());
        obj.addString("bottom", this.bottom.getCSSCode());
        return obj;
    }

    /**
     * Checks equality between this {@code Offset} and another object.
     * Two offsets are considered equal if all four sides have equal sizes.
     *
     * @param obj the object to compare
     * @return {@code true} if all sides are equal, otherwise {@code false}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Offset)) {
            return false;
        }
        final Offset other = (Offset) obj;
        return this.left.equals(other.left)
            && this.right.equals(other.right)
            && this.top.equals(other.top)
            && this.bottom.equals(other.bottom);
    }

    /**
     * Returns a hash code for this offset based on all four sides.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.left, this.right, this.top, this.bottom);
    }
}
