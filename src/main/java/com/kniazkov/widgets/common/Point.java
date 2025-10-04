/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Class representing a point with integer X and Y coordinates.
 */
public final class Point {
    /**
     * The X coordinate.
     */
    public int x;

    /**
     * The Y coordinate.
     */
    public int y;

    @Override
    public String toString() {
        return String.format("{%d, %d}", this.x, this.y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Point)) {
            return false;
        }
        final Point other = (Point) obj;
        return this.x == other.x && this.y == other.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    /**
     * Calculates the integer Euclidean distance to another point.
     * The result is the floor of the actual distance.
     *
     * @param other the point to which the distance is calculated
     * @return the integer part of the Euclidean distance
     */
    public int distanceTo(Point other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return (int) Math.floor(Math.sqrt(dx * dx + dy * dy));
    }
}
