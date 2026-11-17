/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

/**
 * Opt-in Jetty WebSocket adapter. Binds only to loopback; a trusted TLS proxy shares the
 * public HTTP origin and routes /widgets-stream here. Ordinary HTTP/uploads stay separate.
 */
public final class StreamServer implements AutoCloseable {
    /**
     * WebSocket route, deliberately separate from widget HTTP requests.
     */
    public static final String PATH = "/widgets-stream";
    /**
     * Frame/auth version negotiated before any application message.
     */
    public static final String SUBPROTOCOL = "widgets.stream.v1";
    /**
     * Application owning the HTTP browser sessions.
     */
    final Application application;
    /**
     * Immutable budgets.
     */
    final StreamOptions options;
    /**
     * Application receiver.
     */
    final StreamHandler handler;
    /**
     * Independent bounded application callback pool.
     */
    final ThreadPoolExecutor workers;
    /**
     * Auth, revocation, heartbeat and write watchdog.
     */
    private final ScheduledExecutorService timer;
    /**
     * Reserved endpoints, including incomplete upgrades and authentication.
     */
    private final Set<StreamEndpoint> endpoints = ConcurrentHashMap.newKeySet();
    /**
     * Atomic reservation prevents concurrent upgrades from exceeding capacity.
     */
    private final AtomicInteger reservations = new AtomicInteger();
    /**
     * Terminal close fence.
     */
    private final AtomicBoolean closed = new AtomicBoolean();
    /**
     * Async I/O listener, separate from the existing webserver workers.
     */
    private final org.eclipse.jetty.server.Server server;
    /**
     * Loopback connector.
     */
    private final ServerConnector connector;

    /**
     * @param application owning, already-started HTTP application
     * @param options stream limits and external origin
     * @param handler authenticated message consumer
     * @throws Exception when the listener cannot start
     */
    private StreamServer(final Application application, final StreamOptions options,
            final StreamHandler handler) throws Exception {
        this.application = java.util.Objects.requireNonNull(application);
        this.options = java.util.Objects.requireNonNull(options);
        this.handler = java.util.Objects.requireNonNull(handler);
        final Options http = application.streamOptions();
        if (!http.getPublicOrigin().isEmpty()
                && !http.getPublicOrigin().equals(options.publicOrigin())) {
            throw new IllegalArgumentException("HTTP and stream origins must match");
        }
        if (http.getPublicOrigin().isEmpty() && options.publicOrigin().startsWith("https://")) {
            throw new IllegalArgumentException("Configure the HTTPS public origin on HTTP first");
        }
        this.workers = new ThreadPoolExecutor(options.workers(), options.workers(), 0,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(options.tasks()),
            Thread.ofPlatform().daemon().name("widgets-stream-handler-", 0).factory());
        this.timer = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("widgets-stream-watchdog").factory());
        final QueuedThreadPool threads = new QueuedThreadPool(16, 4);
        threads.setName("widgets-stream-io");
        this.server = new org.eclipse.jetty.server.Server(threads);
        this.connector = new ServerConnector(this.server, 1, 1);
        this.connector.setHost("127.0.0.1");
        this.connector.setPort(options.port());
        this.connector.setIdleTimeout(options.timeoutMillis());
        this.server.addConnector(this.connector);
        this.server.setStopTimeout(2000);
        final ContextHandler context = new ContextHandler("/");
        this.server.setHandler(context);
        context.setHandler(WebSocketUpgradeHandler.from(this.server, context, container -> {
            container.setMaxTextMessageSize(options.messageBytes());
            container.setMaxBinaryMessageSize(options.messageBytes());
            container.setIdleTimeout(java.time.Duration.ofMillis(options.timeoutMillis()));
            container.addMapping(PATH, (request, response, callback) -> {
                final BrowserSession owner = this.authorizeUpgrade(request, http);
                if (owner == null || this.closed.get()) {
                    response.setStatus(403);
                    callback.succeeded();
                    return null;
                }
                if (this.reservations.incrementAndGet() > options.connections()) {
                    this.reservations.decrementAndGet();
                    response.setStatus(503);
                    callback.succeeded();
                    return null;
                }
                final StreamEndpoint endpoint = new StreamEndpoint(this, owner);
                this.endpoints.add(endpoint);
                if (this.closed.get()) {
                    endpoint.close();
                    response.setStatus(503);
                    callback.succeeded();
                    return null;
                }
                response.setAcceptedSubProtocol(SUBPROTOCOL);
                response.setExtensions(List.of());
                return endpoint;
            });
        }));
        try {
            this.server.start();
            final long tick = Math.max(25, Math.min(250,
                Math.min(options.authMillis(), options.heartbeatMillis()) / 4));
            this.timer.scheduleWithFixedDelay(this::tick, tick, tick, TimeUnit.MILLISECONDS);
            application.onClose(this::close);
        } catch (final Exception failure) {
            this.close();
            throw failure;
        }
    }

    /**
     * @param application HTTP application owning authentication and adapter cleanup
     * @param options explicit bounded transport configuration
     * @param handler consumer for R36 or an application-specific streaming protocol
     * @return a live loopback adapter, registered for application shutdown
     */
    public static StreamServer start(final Application application, final StreamOptions options,
            final StreamHandler handler) {
        try {
            return new StreamServer(application, options, handler);
        } catch (final Exception failure) {
            throw new IllegalStateException("Cannot start stream listener", failure);
        }
    }

    /**
     * @return actual listener port, for the trusted reverse proxy upstream
     */
    public int getPort() {
        return this.connector.getLocalPort();
    }

    /**
     * Fixed-cardinality diagnostics, with no session IDs, tokens or user messages.
     * @return current connection, executor and outgoing queue gauges
     */
    public Map<String, Long> getMetrics() {
        long bytes = 0;
        long messages = 0;
        for (final StreamEndpoint endpoint : this.endpoints) {
            final long[] queue = endpoint.queued();
            messages += queue[0];
            bytes += queue[1];
        }
        return Map.of("connections", (long) this.reservations.get(),
            "activeHandlers", (long) this.workers.getActiveCount(),
            "queuedHandlers", (long) this.workers.getQueue().size(),
            "outgoingMessages", messages, "outgoingBytes", bytes);
    }

    /**
     * @param request upgrade metadata, before any WebSocket application data
     * @param http authoritative cookie configuration
     * @return existing active browser owner, or null without creating/touching sessions
     */
    private BrowserSession authorizeUpgrade(final ServerUpgradeRequest request,
            final Options http) {
        final var headers = request.getHeaders();
        final List<String> origins = headers.getValuesList("Origin");
        final List<String> sites = headers.getValuesList("Sec-Fetch-Site");
        if (origins.size() != 1
                || !this.options.publicOrigin().equals(ProtocolSecurity.origin(origins.getFirst()))
                || (!sites.isEmpty() && (sites.size() != 1
                    || !"same-origin".equals(sites.getFirst())))
                || request.getHttpURI().getQuery() != null
                || !request.hasSubProtocol(SUBPROTOCOL)) {
            return null;
        }
        String credential = null;
        final String name = ProtocolSecurity.cookieName(http) + "=";
        for (final String line : headers.getValuesList("Cookie")) {
            for (final String part : line.split(";")) {
                final String cookie = part.strip();
                if (cookie.startsWith(name)) {
                    if (credential != null) {
                        return null;
                    }
                    credential = cookie.substring(name.length());
                }
            }
        }
        return this.application.browserSession(credential);
    }

    /**
     * Watchdog never invokes application code or acquires a widget/client monitor.
     */
    private void tick() {
        for (final StreamEndpoint endpoint : this.endpoints) {
            try {
                endpoint.tick();
            } catch (final RuntimeException failure) {
                endpoint.close();
            }
        }
    }

    /**
     * @param endpoint terminal endpoint whose reservation is released exactly once
     */
    void released(final StreamEndpoint endpoint) {
        if (this.endpoints.remove(endpoint)) {
            this.reservations.decrementAndGet();
        }
    }

    /**
     * Stops streams and their executors; ordinary HTTP/application ownership is unchanged.
     */
    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        this.timer.shutdownNow();
        this.endpoints.forEach(endpoint -> {
            endpoint.close();
            endpoint.disconnect();
        });
        this.workers.shutdownNow();
        try {
            this.server.stop();
        } catch (final Exception failure) {
            throw new IllegalStateException("Cannot stop stream listener", failure);
        }
    }
}
