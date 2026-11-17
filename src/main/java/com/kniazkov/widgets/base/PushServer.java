/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.Json;
import com.kniazkov.json.JsonElement;
import com.kniazkov.json.JsonObject;
import com.kniazkov.json.JsonParsingMode;
import com.kniazkov.widgets.common.RMId;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opt-in multiplexed widget transport. Push invalidations wake the existing synchronization
 * protocol, preserving its event IDs, independent update epochs and replay checkpoints.
 * Network writes never run under an application/client/root monitor.
 */
public final class PushServer implements AutoCloseable, StreamHandler {
    /**
     * Hard per-document subscription and in-flight request budget.
     */
    private static final int CHANNELS = 64;
    /**
     * Authoritative HTTP application.
     */
    private final Application application;
    /**
     * Native authenticated listener.
     */
    private final StreamServer stream;
    /**
     * Global bounded protocol workers; slow page handlers do not serialize other channels.
     */
    private final ThreadPoolExecutor workers;
    /**
     * Coalesced revision sampler and disconnected-owner cleanup.
     */
    private final ScheduledExecutorService timer;
    /**
     * Connection states are bounded by the native listener's connection limit.
     */
    private final Map<StreamConnection, Document> documents = new ConcurrentHashMap<>();
    /**
     * Terminal service fence.
     */
    private final AtomicBoolean closed = new AtomicBoolean();
    /**
     * Native frame budget, including the JSON envelope.
     */
    private final int messageBytes;
    /**
     * Maximum queued/running protocol operation age.
     */
    private final long taskNanos;

    /**
     * @param application already-started HTTP application
     * @param options independent listener, worker and outgoing budgets
     */
    private PushServer(final Application application, final StreamOptions options) {
        this.application = application;
        this.messageBytes = options.messageBytes();
        this.taskNanos = TimeUnit.MILLISECONDS.toNanos(options.timeoutMillis());
        this.workers = new ThreadPoolExecutor(options.workers(), options.workers(), 0,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(options.tasks()),
            Thread.ofPlatform().daemon().name("widgets-push-", 0).factory());
        this.timer = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("widgets-push-revisions").factory());
        StreamServer listener = null;
        try {
            listener = StreamServer.start(application, options, this);
            this.stream = listener;
            application.configurePush(this);
            application.onClose(this::close);
            this.timer.scheduleWithFixedDelay(this::tick, 100, 100, TimeUnit.MILLISECONDS);
        } catch (final RuntimeException | Error failure) {
            this.timer.shutdownNow();
            this.workers.shutdownNow();
            if (listener != null) {
                listener.close();
            }
            throw failure;
        }
    }

    /**
     * @param application HTTP session/page owner
     * @param options loopback listener and bounded resource policy
     * @return running opt-in push service; HTTP remains the fallback and upload transport
     */
    public static PushServer start(final Application application, final StreamOptions options) {
        return new PushServer(application, options);
    }

    /**
     * @return actual loopback upstream port
     */
    public int getPort() {
        return this.stream.getPort();
    }

    /**
     * @return terminal service state
     */
    public boolean isClosed() {
        return this.closed.get();
    }

    /**
     * @return advertised native text envelope limit
     */
    int messageBytes() {
        return this.messageBytes;
    }

    /**
     * @return fixed-cardinality resource gauges without identifiers or credentials
     */
    public Map<String, Long> getMetrics() {
        final Map<String, Long> metrics = new java.util.HashMap<>(this.stream.getMetrics());
        metrics.put("subscriptions", this.documents.values().stream()
            .mapToLong(document -> document.channels.size()).sum());
        metrics.put("pushWorkers", (long) this.workers.getActiveCount());
        metrics.put("pushTasks", (long) this.workers.getQueue().size());
        return Map.copyOf(metrics);
    }

    @Override
    public void onText(final StreamConnection connection, final String text) {
        if (this.closed.get() || !connection.isOpen()) {
            connection.close();
            return;
        }
        final Document document = this.documents.computeIfAbsent(connection, Document::new);
        try {
            final JsonObject packet = Json.parse(text, JsonParsingMode.STRICT)
                .toJsonObject().orElseThrow();
            final String type = packet.get("type").stringValue();
            final String name = packet.containsKey("client")
                ? packet.get("client").stringValue() : "";
            if ("subscribe".equals(type)) {
                final JsonObject credentials = packet.get("channel").objectValueOrThrow();
                final ChannelAccess access = new ChannelAccess(connection.owner(),
                    credentials.get("token").stringValue(), credentials.get("epoch").stringValue());
                final Client client = this.application.streamClient(RMId.parse(name), access);
                if (!document.channels.containsKey(name) && document.channels.size() >= CHANNELS) {
                    connection.close();
                    return;
                }
                document.channels.put(name, new Subscription(client, access));
                final JsonObject response = new JsonObject();
                response.addString("type", "subscribed");
                response.addString("client", name);
                connection.send(response.toString());
            } else if ("unsubscribe".equals(type)) {
                document.channels.remove(name);
            } else if ("ackHint".equals(type)) {
                final Subscription subscription = document.channels.get(name);
                if (subscription != null) {
                    synchronized (subscription) {
                        subscription.seen = subscription.client.getRootWidget().getChangeRevision();
                        subscription.notified = false;
                    }
                }
            } else if ("lease".equals(type)) {
                this.submit(document, 0, () -> {
                    for (final Subscription subscription : document.channels.values()) {
                        this.application.renewStreamClient(
                            subscription.client, subscription.access);
                    }
                    connection.send("{\"type\":\"lease\"}");
                });
            } else if ("request".equals(type)) {
                final String identifier = packet.get("id").toString();
                if (!identifier.matches("[1-9][0-9]{0,15}")) {
                    throw new IllegalArgumentException("Invalid request ID");
                }
                final long id = Long.parseLong(identifier);
                if (id > 9007199254740991L || id <= document.lastRequest) {
                    throw new IllegalArgumentException("Non-monotonic request ID");
                }
                document.lastRequest = id;
                final JsonObject query = packet.get("query").objectValueOrThrow();
                final String channel = query.get("client").stringValue();
                final Subscription subscription = document.channels.get(channel);
                if (subscription == null
                        || !"synchronize".equals(query.get("action").stringValue())) {
                    throw new ProtocolAccessException(false);
                }
                final JsonObject credentials = packet.get("channel").objectValueOrThrow();
                final ChannelAccess access = new ChannelAccess(connection.owner(),
                    credentials.get("token").stringValue(), credentials.get("epoch").stringValue());
                this.application.streamClient(subscription.client.getId(), access);
                final Map<String, String> request = new java.util.HashMap<>();
                request.put("client", channel);
                request.put("lastUpdate", query.get("lastUpdate").stringValue());
                request.put("events", query.get("events").toString());
                request.put("updateEpoch", credentials.get("updateEpoch").stringValue());
                this.submit(document, id, () -> {
                    JsonElement data;
                    try {
                        data = new Synchronize(this.application, access).process(request);
                    } catch (final ProtocolAccessException denied) {
                        data = failure(denied.sessionExpired() ? "sessionExpired" : "accessDenied");
                    } catch (final RuntimeException | Error failure) {
                        data = failure("clientError");
                    }
                    final JsonObject response = new JsonObject();
                    response.addString("type", "response");
                    response.addNumber("id", id);
                    response.addElement("data", data);
                    final String encoded = response.toString();
                    if (encoded.getBytes(StandardCharsets.UTF_8).length > this.messageBytes) {
                        connection.send("{\"type\":\"http\",\"id\":" + id + "}");
                    } else {
                        connection.send(encoded);
                    }
                });
            } else {
                connection.close();
            }
        } catch (final Exception invalid) {
            connection.close();
        }
    }

    /**
     * @param key protocol error category, never exception text
     * @return legacy protocol failure envelope
     */
    private static JsonObject failure(final String key) {
        final JsonObject result = new JsonObject();
        result.addBoolean(key, true);
        return result;
    }

    /**
     * @param document physical transport owner
     * @param id request identity, zero reserved for one coalesced lease
     * @param action bounded protocol operation, potentially waiting only on its own page
     */
    private void submit(final Document document, final long id, final Runnable action) {
        if (id == 0 && document.tasks.containsKey(id)) {
            return;
        }
        if (document.tasks.size() >= CHANNELS || document.tasks.containsKey(id)) {
            document.connection.close();
            return;
        }
        final FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                if (document.connection.isOpen() && !this.closed.get()) {
                    action.run();
                }
            } catch (final RuntimeException | Error failure) {
                document.connection.close();
            } finally {
                document.started.remove(id);
                document.tasks.remove(id);
            }
            return null;
        });
        document.tasks.put(id, task);
        document.started.put(id, System.nanoTime());
        try {
            this.workers.execute(task);
        } catch (final java.util.concurrent.RejectedExecutionException busy) {
            document.tasks.remove(id);
            document.started.remove(id);
            document.connection.close();
        }
    }

    /**
     * Reads only atomic revisions; never waits for a slow page handler or sends under its lock.
     */
    private void tick() {
        for (final Document document : this.documents.values()) {
            if (document.started.values().stream()
                    .anyMatch(start -> System.nanoTime() - start >= this.taskNanos)) {
                document.connection.close();
            }
            if (!document.connection.isOpen()) {
                this.documents.remove(document.connection, document);
                document.close();
                continue;
            }
            for (final var entry : document.channels.entrySet()) {
                final Subscription subscription = entry.getValue();
                boolean changed = false;
                synchronized (subscription) {
                    if (!subscription.notified && subscription.seen
                            != subscription.client.getRootWidget().getChangeRevision()) {
                        subscription.notified = true;
                        changed = true;
                    }
                }
                if (changed) {
                    final JsonObject hint = new JsonObject();
                    hint.addString("type", "dirty");
                    hint.addString("client", entry.getKey());
                    document.connection.send(hint.toString());
                }
            }
        }
    }

    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        this.timer.shutdownNow();
        this.documents.values().forEach(Document::close);
        this.documents.clear();
        this.workers.shutdownNow();
        this.stream.close();
        this.application.disablePush(this);
    }

    /**
     * Physical-document ownership; maps have independent hard cardinality limits.
     */
    private final class Document {
        /**
         * Authenticated native transport.
         */
        private final StreamConnection connection;
        /**
         * Strictly increasing native request IDs, read only by the serialized parser callback.
         */
        private long lastRequest;
        /**
         * Independent logical page subscriptions.
         */
        private final Map<String, Subscription> channels = new ConcurrentHashMap<>();
        /**
         * Cancellable operations, including at most one lease.
         */
        private final Map<Long, FutureTask<Void>> tasks = new ConcurrentHashMap<>();
        /**
         * Bounded deadlines for queued/running operations.
         */
        private final Map<Long, Long> started = new ConcurrentHashMap<>();

        /**
         * @param connection physical owner
         */
        Document(final StreamConnection connection) {
            this.connection = connection;
        }

        /**
         * Clears application references and interrupts cooperative handlers.
         */
        void close() {
            this.connection.close();
            this.channels.clear();
            for (final FutureTask<Void> task : this.tasks.values()) {
                task.cancel(true);
                PushServer.this.workers.remove(task);
            }
            this.tasks.clear();
            this.started.clear();
        }
    }

    /**
     * Credentials and invalidation position of one page, never shared with another channel.
     */
    private static final class Subscription {
        /**
         * Authorized page instance.
         */
        private final Client client;
        /**
         * Immutable owner and per-page secret.
         */
        private final ChannelAccess access;
        /**
         * Last acknowledged invalidation generation, distinct from an update ACK.
         */
        private long seen;
        /**
         * At most one outstanding dirty hint until the receiver schedules synchronization.
         */
        private boolean notified;

        /**
         * @param client authorized page
         * @param access immutable channel credentials
         */
        Subscription(final Client client, final ChannelAccess access) {
            this.client = client;
            this.access = access;
            this.seen = client.getRootWidget().getChangeRevision();
        }
    }
}
