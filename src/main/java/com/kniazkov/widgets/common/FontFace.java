/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Represents a font face (i.e., a font family or a predefined keyword).
 * The returned name can either be:
 * <ul>
 *     <li>
 *         a CSS-compatible font family name (e.g., {@code "Arial"}, {@code "Courier New"}); or
 *     </li>
 *     <li>
 *         a special built-in keyword recognized by the rendering engine (e.g., {@code "default"})
 *     </li>
 * </ul>
 */
public interface FontFace {
    /**
     * Default logical font face (typically maps to a system default).
     */
    FontFace DEFAULT = () -> "default";

    /**
     * Returns the font face name.
     * This may be a CSS font family name or a predefined keyword.
     *
     * @return the font face name
     */
    String getName();
}
