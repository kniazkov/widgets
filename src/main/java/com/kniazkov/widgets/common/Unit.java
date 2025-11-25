/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

/**
 * Supported absolute CSS units.
 * <p>
 * These units correspond to standard CSS absolute length measurements.
 */
public enum Unit {

    /** Pixels. */
    PX,

    /** Points (1/72 of an inch). */
    PT,

    /** Picas (1 pica = 12 points). */
    PC,

    /** Inches. */
    IN,

    /** Centimeters. */
    CM,

    /** Millimeters. */
    MM;

    /**
     * Parses unit from string
     * @param s unit as a string
     * @return Unit
     */
    static Unit fromString(final String s) {
        switch (s.toLowerCase()) {
            case "pt": return PT;
            case "pc": return PC;
            case "in": return IN;
            case "cm": return CM;
            case "mm": return MM;
            case "px":
            case "":   return PX;
            default:
                throw new IllegalArgumentException("Unsupported font unit: " + s);
        }
    }
}
