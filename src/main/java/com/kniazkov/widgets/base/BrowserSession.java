/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Server-owned transport session, authenticated by an HttpOnly cookie and a CSRF token.
 * It is not a logged-in user or a substitute for application authorization. Never use
 * the browser-supplied PageContext.browserId as proof of ownership.
 */
public final class BrowserSession {
    /**
     * Public, non-secret identity for application-side associations.
     */
    private final UUID id = UUID.randomUUID();
    /**
     * HttpOnly cookie credential, never exposed to application pages or JavaScript.
     */
    private final String credential = ProtocolSecurity.secret();
    /**
     * Independent bootstrap proof for same-origin POST requests.
     */
    private final String csrf = ProtocolSecurity.secret();
    /**
     * Maximum idle time in nanoseconds.
     */
    private final long lifetime;
    /**
     * Monotonic timestamp of accepted activity.
     */
    private long lastActivity = System.nanoTime();
    /**
     * Terminal revocation flag.
     */
    private boolean revoked;

    /**
     * Creates a new transport owner with independent cookie and CSRF credentials.
     * @param milliseconds maximum idle time
     */
    BrowserSession(final long milliseconds) {
        this.lifetime = TimeUnit.MILLISECONDS.toNanos(milliseconds);
    }

    /**
     * Returns the public identity for application-side associations.
     * @return non-secret transport identity, not a business-user identity
     */
    public UUID getId() {
        return this.id;
    }

    /**
     * Checks idle expiration and terminal revocation without renewing the session.
     * @return whether the session is neither expired nor revoked
     */
    public synchronized boolean isActive() {
        if (System.nanoTime() - this.lastActivity >= this.lifetime) {
            this.revoked = true;
        }
        return !this.revoked;
    }

    /**
     * Permanently invalidates all channels of this browser session. Call when changing
     * application authentication. Already executing business operations cannot be undone;
     * serialize business authorization and mutations in the application itself.
     */
    public synchronized void revoke() {
        this.revoked = true;
    }

    /**
     * @return whether accepted activity renewed a still-live session
     */
    synchronized boolean touch() {
        if (!this.isActive()) {
            return false;
        }
        this.lastActivity = System.nanoTime();
        return true;
    }

    /**
     * @return cookie credential, for the HTTP boundary only
     */
    String credential() {
        return this.credential;
    }

    /**
     * @return bootstrap CSRF token, for the HTTP boundary only
     */
    String csrf() {
        return this.csrf;
    }

    /**
     * @param text untrusted diagnostic text
     * @return text with this session's credentials removed
     */
    String redact(final String text) {
        return text.replace(this.credential, "[redacted]").replace(this.csrf, "[redacted]");
    }
}
