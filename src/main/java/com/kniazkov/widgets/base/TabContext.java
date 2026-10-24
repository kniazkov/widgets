/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Revocable identity shared by pages in one live browser document. This is not
 * authorization: applications must check the real user and business permissions.
 * Full document reloads deliberately create a new context.
 */
public final class TabContext {
    /**
     * Server-generated identity, never reused after revocation or expiration.
     */
    private final UUID id = UUID.randomUUID();

    /**
     * Browser owning this context.
     */
    private final UUID browserId;

    /**
     * Maximum inactivity in nanoseconds.
     */
    private final long lifetime;

    /**
     * Monotonic clock, injectable for expiry tests.
     */
    private final LongSupplier clock;

    /**
     * Last accepted activity.
     */
    private long lastActivity;

    /**
     * Terminal revocation flag.
     */
    private boolean revoked;

    /**
     * Creates an isolated document identity.
     *
     * @param browserId owning browser
     * @param milliseconds inactivity timeout
     */
    TabContext(final UUID browserId, final long milliseconds) {
        this(browserId, milliseconds, System::nanoTime);
    }

    /**
     * Creates an isolated document identity.
     *
     * @param browserId owning browser
     * @param milliseconds inactivity timeout
     * @param clock monotonic clock
     */
    TabContext(final UUID browserId, final long milliseconds, final LongSupplier clock) {
        this.browserId = Objects.requireNonNull(browserId, "browserId");
        this.lifetime = TimeUnit.MILLISECONDS.toNanos(milliseconds);
        this.clock = Objects.requireNonNull(clock, "clock");
        this.lastActivity = clock.getAsLong();
    }

    /**
     * Returns the identity shared by this document's pages.
     *
     * @return stable server-issued context identity
     */
    public UUID getId() {
        return this.id;
    }

    /**
     * Returns the original browser identity.
     *
     * @return owning browser identity (not the selected customer)
     */
    public UUID getBrowserId() {
        return this.browserId;
    }

    /**
     * Checks revocation and idle expiration without renewing the context.
     *
     * @return whether this context can still be used
     */
    public synchronized boolean isActive() {
        if (this.clock.getAsLong() - this.lastActivity >= this.lifetime) {
            this.revoked = true;
        }
        return !this.revoked;
    }

    /**
     * Permanently ends this context. Already executing application operations are not
     * interrupted; applications must serialize their own authorization and mutations.
     * Subsequent protocol events and uploads from its old clients are rejected.
     */
    public synchronized void revoke() {
        this.revoked = true;
    }

    /**
     * Renews only a still-live context; activity never resurrects a revoked identity.
     *
     * @return whether activity was accepted
     */
    synchronized boolean touch() {
        if (!this.isActive()) {
            return false;
        }
        this.lastActivity = this.clock.getAsLong();
        return true;
    }
}
