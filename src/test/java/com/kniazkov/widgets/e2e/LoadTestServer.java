/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.ProtocolLimits;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Link;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Loopback-only load fixture. Metrics are private local files, not a public HTTP route.
 */
public final class LoadTestServer {
    /**
     * No instances.
     */
    private LoadTestServer() { }

    /**
     * @param args port and private metrics journal path
     * @throws Exception if the journal cannot be created
     */
    public static void main(final String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected port and metrics path");
        }
        final Path journal = Path.of(args[1]);
        Files.writeString(journal, "");
        final var states = new ConcurrentHashMap<UUID, State>();
        final Page small = page(states, 0);
        final Application app = new Application(small);
        app.addPage("load", small);
        app.addPage("load-next", small);
        app.addPage("load-large", page(states, 256));
        app.addPage("load-upload", (root, context) -> {
            final TextWidget result = new TextWidget("Uploads: 0");
            final AtomicInteger calls = new AtomicInteger();
            final FileLoader loader = new FileLoader("Upload fixture");
            loader.onSelect(file -> file.onLoad(loaded -> {
                try (var input = loaded.openStream()) {
                    final byte[] bytes = input.readAllBytes();
                    if (bytes.length != 65536) {
                        throw new AssertionError("Unexpected upload length");
                    }
                    for (final byte value : bytes) {
                        if (value != 7) {
                            throw new AssertionError("Corrupt fixture upload");
                        }
                    }
                    Thread.sleep(50);
                    file.executeOnRoot(() -> result.setText("Uploads: " + calls.incrementAndGet()));
                } catch (final Exception error) {
                    throw new IllegalStateException(error);
                }
            }));
            root.add(new Section(loader, result));
        });
        final var defaults = ProtocolLimits.defaults();
        Server.start(app, new Options.Builder().setPort(Integer.parseInt(args[0]))
            .setBindAddress(InetAddress.getLoopbackAddress()).setUploadProcessing(1, 1)
            .setProtocolLimits(new ProtocolLimits(defaults.pendingUpdates(),
                defaults.pendingBytes(),
                defaults.updatesPerBatch(), defaults.updateBytesPerBatch(), 1500,
                defaults.eventsPerBatch(), defaults.eventBytesPerBatch()))
            .build());
        final var sampler = Executors.newSingleThreadScheduledExecutor(task -> {
            final Thread thread = new Thread(task, "load-metrics");
            thread.setDaemon(true);
            return thread;
        });
        final long start = System.nanoTime();
        sampler.scheduleWithFixedDelay(() -> {
            try {
                final JsonObject json = new JsonObject();
                json.addNumber("elapsedMillis", (System.nanoTime() - start) / 1000000);
                app.getMetrics().forEach(json::addNumber);
                Files.writeString(journal, json + "\n", StandardOpenOption.APPEND);
            } catch (final Exception failure) {
                failure.printStackTrace();
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
        app.onClose(sampler::shutdownNow);
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
    }

    /**
     * @param states durable fixture state per document, never used as metric labels
     * @param size extra text widgets
     * @return deterministic page factory
     */
    private static Page page(final ConcurrentHashMap<UUID, State> states, final int size) {
        return (root, context) -> {
            final State state = states.computeIfAbsent(context.tabContext.getId(),
                key -> new State());
            final TextWidget count = new TextWidget(TextWidget.getDefaultStyle(), state.text);
            final Button button = new Button("Increment");
            button.onClick(event -> state.increment());
            root.add(new Section(button, count, new Link("Next load page", "/load-next")));
            for (int index = 0; index < size; index++) {
                root.add(new Section(new TextWidget("Row " + index)));
            }
        };
    }

    /**
     * Test-only business state survives a channel resync.
     */
    private static final class State {
        /**
         * Committed command count.
         */
        private int count;
        /**
         * Shared display model.
         */
        private final StringModel text = new StringModel("Count: 0");
        /**
         * Publishes commands in the same order as the business state.
         */
        synchronized void increment() {
            this.text.setData("Count: " + ++this.count);
        }
    }
}
