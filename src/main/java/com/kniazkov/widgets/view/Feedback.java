/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.protocol.SetFeedback;
import java.util.Objects;

/**
 * Common block message with application-owned optional action widgets.
 * Mutations follow the usual root.execute contract when made from background work.
 */
public abstract class Feedback extends Panel {
    /**
     * Semantic severity, independent of localized message wording.
     */
    public enum Severity {
        /**
         * Neutral information.
         */
        INFO,
        /**
         * Completed operation.
         */
        SUCCESS,
        /**
         * Recoverable warning.
         */
        WARNING,
        /**
         * Persistent important error.
         */
        ERROR
    }

    /**
     * Visible localized message.
     */
    private String message;
    /**
     * Current severity.
     */
    private Severity severity;
    /**
     * Browser representation.
     */
    private final String kind;
    /**
     * Progress percentage, or -1 for indeterminate.
     */
    protected int percentage = -1;
    /**
     * Operation is currently running.
     */
    protected boolean running;

    /**
     * Initializes a message without interpreting user text as HTML.
     * @param kind browser kind
     * @param message localized text
     * @param severity initial severity
     */
    @SuppressWarnings("this-escape")
    protected Feedback(final String kind, final String message, final Severity severity) {
        this.kind = kind;
        this.message = Objects.requireNonNull(message);
        this.severity = Objects.requireNonNull(severity);
        this.publish();
    }

    @Override
    public final String getType() {
        return "feedback";
    }

    /**
     * @return current localized message
     */
    public final String getText() {
        return this.message;
    }

    /**
     * Changes visible text without changing focus.
     * @param message localized plain text
     */
    public final void setText(final String message) {
        this.requireUsable();
        this.message = Objects.requireNonNull(message);
        this.publish();
    }

    /**
     * @return current severity
     */
    public final Severity getSeverity() {
        return this.severity;
    }

    /**
     * Changes semantic severity and presentation together.
     * @param severity desired severity
     */
    public final void setSeverity(final Severity severity) {
        this.requireUsable();
        this.severity = Objects.requireNonNull(severity);
        this.publish();
    }

    /**
     * Sends a new immutable plain-data snapshot.
     */
    protected final void publish() {
        final JsonObject data = new JsonObject();
        data.addString("kind", this.kind);
        data.addString("text", this.message);
        data.addString("severity", this.severity.name().toLowerCase(java.util.Locale.ROOT));
        data.addNumber("progress", this.percentage);
        data.addBoolean("running", this.running);
        this.pushUpdate(new SetFeedback(this.getId(), data));
    }
}
