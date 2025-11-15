/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

public interface ImageSource {
    String toString();

    static ImageSource fromHyperlink(final String href) {
        return new ImageSource() {
            @Override
            public String toString() {
                return href;
            }
        };
    }

    ImageSource INVALID = new ImageSource() {
        @Override
        public String toString() {
            return "#";
        }
    };
}
