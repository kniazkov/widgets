/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

/**
 * ...
 */
public class FileLoader extends Button {
    /**
     * ...
     */
    public FileLoader() {
        super();
    }

    /**
     * ...
     * @param text
     */
    public FileLoader(final String text) {
        super(text);
    }

    /**
     * ...
     * @param style
     * @param text
     */
    public FileLoader(final ButtonStyle style, final String text) {
        super(style, text);
    }

    @Override
    public String getType() {
        return "file loader";
    }
}
