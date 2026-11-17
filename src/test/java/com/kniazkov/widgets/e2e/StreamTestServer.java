/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.ProtocolLimits;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.base.StreamServer;
import com.kniazkov.widgets.base.StreamOptions;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real stream adapter fixture behind the TLS test proxy.
 */
public final class StreamTestServer {
    /**
     * No instances.
     */
    private StreamTestServer() { }

    /**
     * @param args HTTP port, stream port, public origin and private metrics file
     */
    public static void main(final String[] args) {
        final var states = new ConcurrentHashMap<UUID, State>();
        final Application app = new Application((root, context) -> {
            final State state = states.computeIfAbsent(context.tabContext.getId(),
                key -> new State());
            final Button increment = new Button("Increment");
            increment.onClick(event -> state.increment());
            final Button delayed = new Button("Delayed increment");
            delayed.onClick(event -> {
                state.increment();
                try {
                    Thread.sleep(500);
                } catch (final InterruptedException stopped) {
                    Thread.currentThread().interrupt();
                }
            });
            final Button revoke = new Button("Revoke session");
            revoke.onClick(event -> context.browserSession.revoke());
            final TextWidget uploaded = new TextWidget("Uploads: 0");
            final FileLoader loader = new FileLoader("Upload transport fixture");
            loader.onSelect(file -> file.onLoad(loaded -> {
                try (var stream = loaded.openStream()) {
                    final byte[] bytes = stream.readAllBytes();
                    if (bytes.length != 65536) {
                        throw new IllegalStateException("Wrong fixture size");
                    }
                    for (final byte value : bytes) {
                        if (value != 9) {
                            throw new IllegalStateException("Corrupt fixture data");
                        }
                    }
                    file.executeOnRoot(() -> uploaded.setText("Uploads: 1"));
                } catch (final java.io.IOException error) {
                    throw new IllegalStateException(error);
                }
            }));
            root.add(new Section(increment, delayed, revoke,
                new TextWidget(TextWidget.getDefaultStyle(), state.text), loader, uploaded));
        });
        final var limits = ProtocolLimits.defaults();
        Server.start(app, new Options.Builder().setPort(Integer.parseInt(args[0]))
            .setBindAddress(InetAddress.getLoopbackAddress())
            .setPublicOrigin(args[2])
            .setProtocolLimits(new ProtocolLimits(limits.pendingUpdates(), limits.pendingBytes(),
                limits.updatesPerBatch(), limits.updateBytesPerBatch(), 5000,
                limits.eventsPerBatch(), limits.eventBytesPerBatch())).build());
        final var stream = StreamServer.start(app, new StreamOptions(Integer.parseInt(args[1]),
            args[2], 64, 2, 4, 65536, 8, 131072, 1000, 500, 3000), (connection, text) -> {
                if ("hold".equals(text)) {
                    Thread.sleep(10000);
                } else {
                    connection.send(text);
                }
            });
        final var metrics = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        metrics.scheduleWithFixedDelay(() -> {
            try {
                final String values = stream.getMetrics().entrySet().stream()
                    .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue())
                    .collect(java.util.stream.Collectors.joining(",", "{", "}"));
                java.nio.file.Files.writeString(java.nio.file.Path.of(args[3]), values);
            } catch (final java.io.IOException failure) {
                throw new IllegalStateException(failure);
            }
        }, 0, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
        app.onClose(metrics::shutdownNow);
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
    }

    /**
     * Business state survives channel resynchronization, but not a process restart.
     */
    private static final class State {
        /**
         * Authoritative count.
         */
        private int count;
        /**
         * Published value.
         */
        private final StringModel text = new StringModel("Count: 0");
        /**
         * Executes one command.
         */
        synchronized void increment() {
            this.text.setData("Count: " + ++this.count);
        }
    }
}
