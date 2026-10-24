/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.view;

import com.kniazkov.widgets.protocol.SetCountdownDeadline;
import java.time.Instant;
import java.util.Objects;

/**
 * Displays remaining server time as MM:SS, or H:MM:SS for durations of at least one hour.
 * Fractions round up, past deadlines display zero, and expiration has no server-side effect.
 * The browser uses monotonic elapsed time and the existing transport's server-time samples.
 * Configure typography through {@link TextWidgetStyle}; the displayed text is client-owned.
 */
public final class Countdown extends InlineWidget<TextWidgetStyle> implements HasColor {
    /**
     * Current absolute server deadline, with millisecond precision.
     */
    private Instant deadline;

    /**
     * Creates a countdown with the default text style.
     *
     * @param deadline absolute server deadline
     */
    public Countdown(final Instant deadline) {
        this(TextWidget.getDefaultStyle(), deadline);
    }

    /**
     * Creates a styled countdown.
     *
     * @param style text appearance
     * @param deadline absolute server deadline
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the deadline is outside the browser date range
     */
    public Countdown(final TextWidgetStyle style, final Instant deadline) {
        super(Objects.requireNonNull(style, "style"));
        this.setDeadline(deadline);
    }

    @Override
    public String getType() {
        return "countdown";
    }

    /**
     * Returns the current deadline.
     *
     * @return server deadline, truncated to milliseconds
     */
    public synchronized Instant getDeadline() {
        return this.deadline;
    }

    /**
     * Replaces the deadline, including after the display has reached zero.
     * No server timer or expiration event is created.
     *
     * @param value absolute server deadline
     * @throws NullPointerException if the deadline is null
     * @throws IllegalArgumentException if the deadline is outside the browser date range
     */
    public synchronized void setDeadline(final Instant value) {
        Objects.requireNonNull(value, "deadline");
        final long millis;
        try {
            millis = value.toEpochMilli();
        } catch (final ArithmeticException exception) {
            throw new IllegalArgumentException("Deadline is outside the browser date range",
                exception);
        }
        if (millis < -8640000000000000L || millis > 8640000000000000L) {
            throw new IllegalArgumentException("Deadline is outside the browser date range");
        }
        this.deadline = Instant.ofEpochMilli(millis);
        this.pushUpdate(new SetCountdownDeadline(this.getId(), millis));
    }
}
