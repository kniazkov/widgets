/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import java.util.Objects;

/**
 * Operation progress with optional cooperative cancellation, never a background job owner.
 * A completed/cancelled instance is not reused; start a new instance for a new operation.
 */
public class Progress extends Feedback {
    /**
     * Cancellation action, cleared before invocation and on retirement.
     */
    private Runnable cancellation;
    /**
     * Owned optional cancel button.
     */
    private Button cancelButton;

    /**
     * Creates indeterminate running progress.
     * @param text localized operation name
     */
    @SuppressWarnings("this-escape")
    public Progress(final String text) {
        super("progress", text, Severity.INFO);
        this.running = true;
        this.onRetire(() -> {
            this.cancellation = null;
            this.running = false;
        });
        this.publish();
    }

    /**
     * @return percentage, or -1 for indeterminate
     */
    public final int getPercentage() {
        return this.percentage;
    }

    /**
     * @return whether this operation is still running
     */
    public final boolean isRunning() {
        return this.running;
    }

    /**
     * Updates progress; completion remains an explicit application decision.
     * @param value -1 or a percentage from 0 through 100
     */
    public final void setPercentage(final int value) {
        this.requireUsable();
        if (value < -1 || value > 100) {
            throw new IllegalArgumentException("Progress must be -1 or 0..100");
        }
        if (!this.running) {
            throw new IllegalStateException("Operation is terminal");
        }
        this.percentage = value;
        this.publish();
    }

    /**
     * Adds a once-only cancellation request. The application must stop its own work.
     * @param caption localized button caption
     * @param cancelled localized terminal message
     * @param action cooperative cancellation handler
     */
    public final void onCancel(final String caption, final String cancelled,
            final Runnable action) {
        this.requireUsable();
        if (!this.running || this.cancelButton != null) {
            throw new IllegalStateException("Cancellation is already configured or terminal");
        }
        Objects.requireNonNull(cancelled);
        this.cancellation = Objects.requireNonNull(action);
        this.cancelButton = new Button(Objects.requireNonNull(caption));
        this.cancelButton.onClick(event -> {
            if (this.running && this.cancellation != null) {
                final Runnable callback = this.cancellation;
                this.finish(cancelled, Severity.INFO);
                callback.run();
            }
        });
        this.add(new Section(this.cancelButton));
    }

    /**
     * Ends progress, removing busy semantics and rejecting later progress updates.
     * @param message localized terminal state
     * @param severity success, error or cancelled presentation
     */
    public final void finish(final String message, final Severity severity) {
        this.requireUsable();
        Objects.requireNonNull(message);
        Objects.requireNonNull(severity);
        if (!this.running) {
            return;
        }
        this.running = false;
        this.cancellation = null;
        if (this.cancelButton != null) {
            this.cancelButton.disable();
        }
        this.setSeverity(severity);
        this.setText(message);
    }
}
