/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonParsingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

/**
 * Internal adapter endpoint, public only for Jetty method-handle access.
 * One demand-driven endpoint; no widget/client monitor is held while waiting for I/O.
 */
public final class StreamEndpoint implements StreamConnection, Session.Listener {
    /**
     * Owning adapter.
     */
    private final StreamServer server;
    /**
     * Cookie-bound owner, validated again on AUTH, receive, send and watchdog.
     */
    private final BrowserSession owner;
    /**
     * Outgoing text and byte counts, including the current asynchronous write.
     */
    private final ArrayDeque<String> output = new ArrayDeque<>();
    /**
     * Current byte reservation.
     */
    private long outputBytes;
    /**
     * Socket, published by Jetty after upgrade.
     */
    private volatile Session socket;
    /**
     * Terminal lifecycle fence.
     */
    private volatile boolean closed;
    /**
     * Close write deadline, checked by the existing watchdog without per-close timer tasks.
     */
    private volatile long closing;
    /**
     * Successful bootstrap authentication.
     */
    private volatile boolean authenticated;
    /**
     * Reservation time also bounds an incomplete upgrade.
     */
    private final long created = System.nanoTime();
    /**
     * Last successful heartbeat.
     */
    private volatile long heartbeat = this.created;
    /**
     * Outstanding ping timestamp, zero when no pong is awaited.
     */
    private volatile long ping;
    /**
     * Asynchronous writer start, zero while idle.
     */
    private volatile long writing;
    /**
     * Queued/running application callback start, zero while idle.
     */
    private volatile long handling;
    /**
     * Cancellable queued/running callback.
     */
    private volatile FutureTask<Void> task;

    /**
     * @param server owner and immutable budgets
     * @param owner cookie-authenticated browser
     */
    StreamEndpoint(final StreamServer server, final BrowserSession owner) {
        this.server = server;
        this.owner = owner;
    }

    @Override
    public BrowserSession owner() {
        return this.owner;
    }

    @Override
    public boolean isOpen() {
        return !this.closed && this.authenticated && this.owner.isActive()
            && !this.server.application.isClosed();
    }

    @Override
    public void onWebSocketOpen(final Session session) {
        this.socket = session;
        if (this.closed) {
            session.disconnect();
        } else {
            session.demand();
        }
    }

    @Override
    public void onWebSocketText(final String text) {
        if (this.closed || !this.owner.isActive()) {
            this.finish(1008);
            return;
        }
        if (!this.authenticated) {
            this.authenticate(text);
            if (this.isOpen()) {
                this.socket.demand();
            }
            return;
        }
        this.handling = System.nanoTime();
        final FutureTask<Void> work = new FutureTask<>(() -> {
            try {
                if (this.isOpen() && this.owner.touch()) {
                    this.server.handler.onText(this, text);
                } else {
                    this.finish(1008);
                }
            } catch (final Exception failure) {
                this.finish(1011);
            } finally {
                this.handling = 0;
                this.task = null;
                if (this.isOpen()) {
                    this.socket.demand();
                }
            }
            return null;
        });
        this.task = work;
        try {
            this.server.workers.execute(work);
            if (this.closed) {
                work.cancel(true);
                this.server.workers.remove(work);
            }
        } catch (final RejectedExecutionException full) {
            this.finish(1013);
        }
    }

    /**
     * @param text first frame, never passed to application code
     */
    private void authenticate(final String text) {
        try {
            if (text.length() > 1024) {
                this.finish(1008);
                return;
            }
            final var value = Json.parse(text, JsonParsingMode.STRICT).toJsonObject().orElseThrow();
            if (!"auth".equals(value.get("type").stringValue())
                    || !"1".equals(value.get("version").toString())
                    || !this.owner.getId().toString().equals(value.get("session").stringValue())
                    || !ProtocolSecurity.matches(this.owner.csrf(), value.get("csrf").stringValue())
                    || !this.owner.touch()) {
                this.finish(1008);
                return;
            }
            this.authenticated = true;
            this.send("{\"type\":\"ready\",\"version\":1,\"serverEpoch\":\""
                + this.server.application.getServerId() + "\"}");
        } catch (final Exception invalid) {
            this.finish(1008);
        }
    }

    @Override
    public void onWebSocketBinary(final ByteBuffer payload, final Callback callback) {
        callback.succeed();
        this.finish(1003);
    }

    @Override
    public void onWebSocketPing(final ByteBuffer payload) {
        if (!this.closed) {
            this.socket.demand();
        }
    }

    @Override
    public void onWebSocketPong(final ByteBuffer payload) {
        if (this.ping != 0 && payload.remaining() == Long.BYTES
                && payload.getLong(payload.position()) == this.ping) {
            this.ping = 0;
            this.heartbeat = System.nanoTime();
        }
        if (!this.closed) {
            this.socket.demand();
        }
    }

    @Override
    public void onWebSocketError(final Throwable failure) {
        this.finish(1011);
    }

    @Override
    public void onWebSocketClose(final int code, final String reason, final Callback callback) {
        this.finish(code);
        this.server.released(this);
        callback.succeed();
    }

    @Override
    public boolean send(final String text) {
        java.util.Objects.requireNonNull(text);
        if (!this.isOpen()) {
            this.finish(1008);
            return false;
        }
        if (text.length() > this.server.options.messageBytes()) {
            this.finish(1009);
            return false;
        }
        final int bytes = text.getBytes(StandardCharsets.UTF_8).length;
        boolean start = false;
        boolean overflow = false;
        synchronized (this) {
            if (this.closed) {
                return false;
            }
            if (bytes > this.server.options.messageBytes()
                    || this.output.size() >= this.server.options.queuedMessages()
                    || bytes > this.server.options.queuedBytes() - this.outputBytes) {
                overflow = true;
            } else {
                start = this.output.isEmpty();
                this.output.addLast(text);
                this.outputBytes += bytes;
            }
        }
        if (overflow) {
            this.finish(1013);
            return false;
        }
        if (start) {
            this.write(text);
        }
        return true;
    }

    /**
     * @param text reserved queue head, sent without holding the queue monitor
     */
    private void write(final String text) {
        if (!this.isOpen()) {
            this.finish(1008);
            return;
        }
        this.writing = System.nanoTime();
        try {
            this.socket.sendText(text, Callback.from(() -> this.written(text),
                failure -> this.finish(1011)));
        } catch (final RuntimeException failure) {
            this.finish(1011);
        }
    }

    /**
     * @param text completed queue head
     */
    private void written(final String text) {
        final String next;
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.output.removeFirst();
            this.outputBytes -= text.getBytes(StandardCharsets.UTF_8).length;
            next = this.output.peekFirst();
            this.writing = 0;
        }
        if (next != null) {
            this.write(next);
        }
    }

    /**
     * @return pending message and byte reservations, including the current write
     */
    synchronized long[] queued() {
        return new long[]{this.output.size(), this.outputBytes};
    }

    /**
     * Checks deadlines outside all application/widget locks.
     */
    void tick() {
        final long now = System.nanoTime();
        final long deadline = TimeUnit.MILLISECONDS.toNanos(this.server.options.timeoutMillis());
        if (this.closed) {
            if (now - this.closing >= TimeUnit.SECONDS.toNanos(1)) {
                this.disconnect();
            }
        } else if (!this.owner.isActive() || this.server.application.isClosed()) {
            this.finish(1008);
        } else if (!this.authenticated
                && now - this.created >= TimeUnit.MILLISECONDS.toNanos(
                    this.server.options.authMillis())) {
            this.finish(1008);
        } else if ((this.ping != 0 && now - this.ping >= deadline)
                || (this.writing != 0 && now - this.writing >= deadline)
                || (this.handling != 0 && now - this.handling >= deadline)) {
            this.finish(1001);
        } else if (this.isOpen() && this.ping == 0
                && now - this.heartbeat >= TimeUnit.MILLISECONDS.toNanos(
                    this.server.options.heartbeatMillis())) {
            this.ping = now;
            this.socket.sendPing(ByteBuffer.allocate(Long.BYTES).putLong(0, now),
                Callback.from(() -> { }, failure -> this.finish(1011)));
        }
    }

    /**
     * Releases logical resources immediately and forces physical cancellation, without
     * waiting for an unresponsive peer to acknowledge a close handshake.
     * @param code terminal category; no user data or exception details in the close reason
     */
    private void finish(final int code) {
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.closing = System.nanoTime();
            this.closed = true;
            this.output.clear();
            this.outputBytes = 0;
        }
        final FutureTask<Void> pending = this.task;
        if (pending != null) {
            pending.cancel(true);
            this.server.workers.remove(pending);
        }
        final Session session = this.socket;
        if (session == null) {
            this.server.released(this);
        } else {
            try {
                session.close(code, "stream closed", Callback.from(this::disconnect,
                    failure -> this.disconnect()));
            } catch (final RuntimeException failure) {
                this.disconnect();
            }
        }
    }

    /**
     * Physically cancels stalled writes and releases the reservation exactly once.
     */
    void disconnect() {
        try {
            if (this.socket != null) {
                this.socket.disconnect();
            }
        } finally {
            this.server.released(this);
        }
    }

    @Override
    public void close() {
        this.finish(1001);
    }
}
