/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.base.StaticSource;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.net.InetAddress;

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
                descriptor.onLoad(file -> result.setText(
                    "Loaded " + file.getName() + " (" + file.getSize() + " bytes)"
                ));
            });
            root.add(new Section(loader));
            root.add(new Section(result));
        });
        Server.start(app, new Options.Builder()
            .setPort(Integer.parseInt(args[0]))
            .setBindAddress(InetAddress.getLoopbackAddress())
            .setChunkSize(MEBIBYTE)
            .setMaxFileSize(MEBIBYTE)
            .addStaticSource(StaticSource.classpath(
                "/upload-fixture", UploadBoundaryServer.class, "/upload-boundary"))
            .build());
    }
}
