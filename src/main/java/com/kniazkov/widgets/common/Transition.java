/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.common;

import java.util.Objects;

/**
 * Immutable transition applied to all animatable widget properties.
 */
public final class Transition {
    /**
     * No transition.
     */
    public static final Transition NONE = new Transition(0);

    /**
     * Duration in milliseconds.
     */
    private final int duration;

    /**
     * Delay in milliseconds.
     */
    private final int delay;

    /**
     * Motion curve.
     */
    private final TimingFunction timingFunction;

    /**
     * Creates an ease transition without a delay.
     *
     * @param duration duration in milliseconds
     */
    public Transition(final int duration) {
        this(duration, TimingFunction.EASE, 0);
    }

    /**
     * Creates a transition without a delay.
     *
     * @param duration duration in milliseconds
     * @param timingFunction motion curve
     */
    public Transition(final int duration, final TimingFunction timingFunction) {
        this(duration, timingFunction, 0);
    }

    /**
     * Creates a transition.
     *
     * @param duration duration in milliseconds
     * @param timingFunction motion curve
     * @param delay delay in milliseconds
     */
    public Transition(final int duration, final TimingFunction timingFunction, final int delay) {
        if (duration < 0 || delay < 0) {
            throw new IllegalArgumentException("Transition duration and delay must be >= 0");
        }
        this.duration = duration;
        this.timingFunction = Objects.requireNonNull(timingFunction, "timingFunction");
        this.delay = delay;
    }

    /**
     * Returns the duration.
     *
     * @return duration in milliseconds
     */
    public int getDuration() {
        return this.duration;
    }

    /**
     * Returns the delay.
     *
     * @return delay in milliseconds
     */
    public int getDelay() {
        return this.delay;
    }

    /**
     * Returns the motion curve.
     *
     * @return timing function
     */
    public TimingFunction getTimingFunction() {
        return this.timingFunction;
    }

    /**
     * Returns CSS code for this transition.
     *
     * @return CSS transition value
     */
    public String getCSSCode() {
        if (this.duration == 0 && this.delay == 0) {
            return "none";
        }
        return "all " + this.duration + "ms " + this.timingFunction.getCSSCode() + " "
            + this.delay + "ms";
    }

    @Override
    public String toString() {
        return this.getCSSCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Transition)) {
            return false;
        }
        final Transition other = (Transition) obj;
        return this.duration == other.duration && this.delay == other.delay
            && this.timingFunction == other.timingFunction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.duration, this.delay, this.timingFunction);
    }
}
