/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import com.kniazkov.json.JsonObject;
import java.util.Objects;

/**
 * Immutable outline definition including the distance from the element border.
 */
public final class Outline {
    /**
     * No outline.
     */
    public static final Outline NONE = new Outline(
        Color.TRANSPARENT, BorderStyle.NONE, new AbsoluteSize(0), 0
    );

    /**
     * Outline color.
     */
    private final Color color;

    /**
     * Outline line style.
     */
    private final BorderStyle style;

    /**
     * Outline width.
     */
    private final AbsoluteSize width;

    /**
     * Distance from the border in pixels.
     */
    private final int offset;

    /**
     * Creates an outline measured in pixels.
     *
     * @param color outline color
     * @param style outline line style
     * @param width outline width in pixels
     * @param offset distance from the border in pixels
     */
    public Outline(final Color color, final BorderStyle style, final int width,
            final int offset) {
        this(color, style, new AbsoluteSize(width), offset);
    }

    /**
     * Creates an outline.
     *
     * @param color outline color
     * @param style outline line style
     * @param width outline width
     * @param offset distance from the border in pixels
     */
    public Outline(final Color color, final BorderStyle style, final AbsoluteSize width,
            final int offset) {
        this.color = Objects.requireNonNull(color, "color");
        this.style = Objects.requireNonNull(style, "style");
        this.width = Objects.requireNonNull(width, "width");
        this.offset = offset;
    }

    /**
     * Returns the outline color.
     *
     * @return outline color
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Returns the line style.
     *
     * @return outline line style
     */
    public BorderStyle getStyle() {
        return this.style;
    }

    /**
     * Returns the outline width.
     *
     * @return outline width
     */
    public AbsoluteSize getWidth() {
        return this.width;
    }

    /**
     * Returns the distance from the border.
     *
     * @return outline offset in pixels
     */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Serializes the outline for the client protocol.
     *
     * @return JSON representation
     */
    public JsonObject toJsonObject() {
        final JsonObject object = new JsonObject();
        object.addElement("color", this.color.toJsonObject());
        object.addString("style", this.style.getCSSCode());
        object.addString("width", this.width.getCSSCode());
        object.addString("offset", this.offset + "px");
        return object;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Outline)) {
            return false;
        }
        final Outline other = (Outline) obj;
        return this.offset == other.offset && this.color.equals(other.color)
            && this.style == other.style
            && this.width.getCSSCode().equals(other.width.getCSSCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.color, this.style, this.width.getCSSCode(), this.offset);
    }
}
