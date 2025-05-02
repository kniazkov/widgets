/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

/**
 * A {@link Controller} implementation that does nothing.
 * <p>
 *     This is a placeholder or default controller used when no action is required
 *     in response to an event. It is safe to assign this controller in all situations
 *     where a non-null {@link Controller} is required but no actual behavior is needed.
 * </p>
 */
public final class StubController implements Controller {
    /**
     * Singleton instance of the stub controller.
     */
    public static final Controller INSTANCE = new StubController();

    /**
     * Private constructor to enforce singleton usage.
     */
    private StubController() {
    }

    /**
     * Does nothing when invoked.
     */
    @Override
    public void handleEvent() {

    }
}
