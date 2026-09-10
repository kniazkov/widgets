/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * A font family loaded through the Google Fonts CSS API v2.
 */
public final class GoogleFont implements WebFont {
    /**
     * Google Fonts CSS API v2 endpoint.
     */
    private static final String ENDPOINT = "https://fonts.googleapis.com/css2?family=";

    /**
     * Font family name.
     */
    private final String family;

    /**
     * Requested weights in ascending order.
     */
    private final int[] weights;

    /**
     * Reusable CSS font face.
     */
    private final FontFace fontFace;

    /**
     * Creates a font request containing the normal 400 weight.
     *
     * @param family Google Fonts family name
     */
    public GoogleFont(final String family) {
        this(family, FontWeight.NORMAL);
    }

    /**
     * Creates a font request containing the specified weights.
     * Duplicate weights are removed and the generated request is sorted.
     *
     * @param family Google Fonts family name
     * @param weights one or more font weights
     */
    public GoogleFont(final String family, final FontWeight... weights) {
        this.family = requireFamily(family);
        Objects.requireNonNull(weights, "Font weights must not be null");
        if (weights.length == 0) {
            throw new IllegalArgumentException("At least one font weight is required");
        }
        this.weights = Arrays.stream(weights)
            .map(value -> Objects.requireNonNull(
                value,
                "Font weight must not be null"
            ))
            .mapToInt(FontWeight::getWeight)
            .distinct()
            .sorted()
            .toArray();
        this.fontFace = new FamilyFontFace(this.family);
    }

    /**
     * Returns the Google Fonts family name.
     *
     * @return family name
     */
    public String getFamily() {
        return this.family;
    }

    /**
     * Returns a defensive copy of the requested numeric CSS weights.
     *
     * @return sorted weights
     */
    public int[] getWeights() {
        return Arrays.copyOf(this.weights, this.weights.length);
    }

    @Override
    public URI getStylesheetUri() {
        final String encoded = URLEncoder.encode(
            this.family,
            StandardCharsets.UTF_8
        );
        final StringBuilder result = new StringBuilder(ENDPOINT)
            .append(encoded)
            .append(":wght@");
        for (int index = 0; index < this.weights.length; index++) {
            if (index > 0) {
                result.append(';');
            }
            result.append(this.weights[index]);
        }
        return URI.create(result.append("&display=swap").toString());
    }

    @Override
    public FontFace getFontFace() {
        return this.fontFace;
    }

    /**
     * Validates and normalizes a Google Fonts family name.
     *
     * @param value family name
     * @return trimmed family name
     */
    private static String requireFamily(final String value) {
        final String family = Objects.requireNonNull(
            value,
            "Font family must not be null"
        ).trim();
        if (family.isEmpty()) {
            throw new IllegalArgumentException("Font family must not be empty");
        }
        for (int index = 0; index < family.length(); index++) {
            final char symbol = family.charAt(index);
            if (symbol > 127 || (!Character.isLetterOrDigit(symbol)
                && symbol != ' ')) {
                throw new IllegalArgumentException(
                    "Invalid Google Fonts family name: " + family
                );
            }
        }
        return family;
    }

    /**
     * CSS font family value corresponding to the loaded Google family.
     */
    private static final class FamilyFontFace implements FontFace {
        /**
         * Quoted CSS family name.
         */
        private final String name;

        /**
         * Creates the CSS value for a family.
         *
         * @param family validated family name
         */
        FamilyFontFace(final String family) {
            this.name = "'" + family + "'";
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
