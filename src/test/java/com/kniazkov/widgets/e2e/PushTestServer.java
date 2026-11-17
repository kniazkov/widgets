/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.PushServer;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.base.StreamOptions;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Same business fixture for HTTP and native push, with two cached page addresses per document.
 */
public final class PushTestServer {
    /**
     * No instances.
     */
    private PushTestServer() { }

    /**
     * @param args HTTP port, stream port, public origin, journal and push/http mode
     */
    public static void main(final String[] args) {
        final var states = new ConcurrentHashMap<UUID, State>();
        final var effects = new AtomicInteger();
        final var pages = new AtomicInteger();
        final var background = Executors.newSingleThreadScheduledExecutor();
        final Application app = new Application(page("A", "/other", states, effects,
            pages, background));
        app.addPage("other", page("B", "/", states, effects, pages, background));
        Server.start(app, new Options.Builder().setPort(Integer.parseInt(args[0]))
            .setBindAddress(InetAddress.getLoopbackAddress()).setPublicOrigin(args[2]).build());
        final PushServer push = "push".equals(args[4]) ? PushServer.start(app,
            StreamOptions.defaults(Integer.parseInt(args[1]), args[2])) : null;
        background.scheduleWithFixedDelay(() -> {
            try {
                final var values = new java.util.TreeMap<String, Long>();
                if (push != null) {
                    values.putAll(push.getMetrics());
                }
                values.put("effects", (long) effects.get());
                values.put("pages", (long) pages.get());
                values.put("heapBytes", java.lang.management.ManagementFactory.getMemoryMXBean()
                    .getHeapMemoryUsage().getUsed());
                final String json = values.entrySet().stream()
                    .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue())
                    .collect(java.util.stream.Collectors.joining(",", "{", "}"));
                java.nio.file.Files.writeString(java.nio.file.Path.of(args[3]), json);
            } catch (final java.io.IOException error) {
                throw new IllegalStateException(error);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        app.onClose(background::shutdownNow);
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
    }

    /**
     * @param title distinct page label
     * @param next next internal URL
     * @param states per-document business model
     * @param effects observable mutation counter
     * @param pages observable page construction counter
     * @param background delayed server-side updates
     * @return page factory shared by both transports
     */
    private static Page page(final String title, final String next,
            final ConcurrentHashMap<UUID, State> states, final AtomicInteger effects,
            final AtomicInteger pages, final ScheduledExecutorService background) {
        return (root, context) -> {
            pages.incrementAndGet();
            final State state = states.computeIfAbsent(context.tabContext.getId(),
                key -> new State(effects));
            final Button increment = new Button("Increment");
            increment.onClick(event -> state.increment());
            final Button delayed = new Button("Delayed increment");
            delayed.onClick(event -> {
                try {
                    Thread.sleep(500);
                    state.increment();
                } catch (final InterruptedException stopped) {
                    Thread.currentThread().interrupt();
                }
            });
            final Button later = new Button("Background increment");
            later.onClick(event -> background.schedule(() -> root.execute(state::increment),
                1000, TimeUnit.MILLISECONDS));
            final Button navigate = new Button("Next page");
            navigate.onClick(event -> root.goToPage(next));
            final Button revoke = new Button("Revoke session");
            revoke.onClick(event -> context.browserSession.revoke());
            final TextWidget uploaded = new TextWidget("Uploads: 0");
            final FileLoader loader = new FileLoader("Upload fixture");
            loader.onSelect(file -> file.onLoad(loaded -> {
                try (var input = loaded.openStream()) {
                    final byte[] bytes = input.readAllBytes();
                    if (bytes.length != 65536) {
                        throw new IllegalStateException("Wrong upload size");
                    }
                    for (final byte value : bytes) {
                        if (value != 9) {
                            throw new IllegalStateException("Corrupt upload");
                        }
                    }
                    file.executeOnRoot(() -> uploaded.setText("Uploads: 1"));
                } catch (final java.io.IOException error) {
                    throw new IllegalStateException(error);
                }
            }));
            root.add(new Section(new TextWidget("Page " + title), increment, delayed, later,
                navigate, revoke, new TextWidget(TextWidget.getDefaultStyle(), state.text),
                loader, uploaded));
        };
    }

    /**
     * Business state shared by cached pages of one document, never by two independent tabs.
     */
    private static final class State {
        /**
         * Count of committed actions, used by the test journal only.
         */
        private final AtomicInteger effects;
        /**
         * Current document counter.
         */
        private int count;
        /**
         * Strongly retained reactive model.
         */
        private final StringModel text = new StringModel("Count: 0");

        /**
         * @param effects fixture-wide observer
         */
        State(final AtomicInteger effects) {
            this.effects = effects;
        }

        /**
         * Applies one observable business mutation.
         */
        synchronized void increment() {
            this.effects.incrementAndGet();
            this.text.setData("Count: " + ++this.count);
        }
    }
}
