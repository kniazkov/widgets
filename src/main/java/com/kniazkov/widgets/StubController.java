/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * Controller that does nothing.
 */
public final class StubController implements Controller {
    /**
     * The instance.
     */
    public static final Controller INSTANCE = new StubController();

    /**
     * Private constructor.
     */
    private StubController() {
    }

    @Override
    public void handleEvent() {
        // do nothing
    }
}
