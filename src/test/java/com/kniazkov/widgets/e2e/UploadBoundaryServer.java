/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.ProtocolLimits;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.base.StaticSource;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;

/**
 * Dedicated browser fixture for the largest supported upload chunk and a one-chunk file limit.
 */
public final class UploadBoundaryServer {
    /**
     * Binary size of one mebibyte.
     */
    private static final int MEBIBYTE = 1024 * 1024;

    /**
     * No instances.
     */
    private UploadBoundaryServer() {
    }

    /**
     * Starts a test-only loopback server.
     *
     * @param args one listening port
     */
    public static void main(final String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected one HTTP port argument");
        }
        final var app = new Application((root, context) -> {
            final TextWidget result = new TextWidget("Choose a sample file");
            final FileLoader loader = new FileLoader("Upload sample file");
            loader.onSelect(descriptor -> {
                result.setText("Selected " + descriptor.getName());
                descriptor.onLoad(file -> descriptor.executeOnRoot(() -> result.setText(
                    "Loaded " + file.getName() + " (" + file.getSize() + " bytes)"
                )));
            });
            root.add(new Section(loader));
            root.add(new Section(result));
        });
        addQueuePage(app);
        addBackpressurePage(app);
        Server.start(app, new Options.Builder()
            .setPort(Integer.parseInt(args[0]))
            .setUploadProcessing(1, 1)
            .setProtocolLimits(new ProtocolLimits(512, 1024 * 1024, 32, 512 * 1024, 60000,
                4, 16 * 1024))
            .setBindAddress(InetAddress.getLoopbackAddress())
            .setChunkSize(MEBIBYTE)
            .setMaxFileSize(MEBIBYTE)
            .addStaticSource(StaticSource.classpath(
                "/upload-fixture", UploadBoundaryServer.class, "/upload-boundary"))
            .build());
    }

    /**
     * Exercises an ordinary burst and rebuilds from application state, not from replayed clicks.
     *
     * @param app test fixture
     */
    private static void addBackpressurePage(final Application app) {
        final var visits = new java.util.concurrent.atomic.AtomicInteger();
        final var committed = new java.util.concurrent.atomic.AtomicInteger();
        app.addPage("protocol-backpressure", (root, context) -> {
            final var opened = new TextWidget("Snapshot " + visits.incrementAndGet());
            final var durable = new TextWidget("Confirmed " + committed.get());
            final var burst = new Button("Save and refresh under pressure");
            final var ordinary = new Button("Save normally");
            final var draft = new TextWidget("Current page");
            ordinary.onClick(event -> durable.setText("Confirmed " + committed.incrementAndGet()));
            burst.onClick(event -> {
                committed.incrementAndGet();
                for (int index = 0; index <= 512; index++) {
                    draft.setText("Update " + index);
                }
            });
            root.add(new Section(opened, durable, ordinary, burst, draft));
        });
    }

    /**
     * Uses one worker plus one queue slot and an explicit, user-controlled release.
     *
     * @param app local test server application
     */
    private static void addQueuePage(final Application app) {
        app.addPage("bounded-processing-queue", (root, context) -> {
            final CountDownLatch release = new CountDownLatch(1);
            root.onClose(release::countDown);
            final var status = new TextWidget("No callback running");
            final var releaseButton = new Button("Release processor");
            releaseButton.onClick(event -> release.countDown());
            final var loader = new FileLoader("Choose queue sample");
            loader.setMultipleInputFlag(true);
            loader.onSelect(descriptor -> descriptor.onLoad(file -> {
                descriptor.executeOnRoot(() -> status.setText("Active " + file.getName()));
                try {
                    release.await();
                } catch (final InterruptedException stopped) {
                    Thread.currentThread().interrupt();
                    return;
                }
                descriptor.executeOnRoot(() -> status.setText("Processed " + file.getName()));
            }));
            root.add(new Section(loader, releaseButton, status));
        });
    }
}
