/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Test-only application used by the Playwright end-to-end suite.
 */
public final class E2ETestServer {
    /**
     * Prevents construction of the static test entry point.
     */
    private E2ETestServer() {
        /*
         * Static entry point only.
         */
    }

    /**
     * Starts the test application on the port supplied by the cross-platform Node runner.
     *
     * @param args a single HTTP port argument
     */
    public static void main(final String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected one HTTP port argument");
        }

        final Page page = (root, context) -> {
            final Section content = new Section();
            final TextWidget status = new TextWidget("Waiting for browser event");
            final Button button = new Button("Run full chain");
            final FileLoader loader = new FileLoader("Upload binary files");
            loader.setMultipleInputFlag(true);

            button.onClick(event -> status.setText("Java handled the click"));
            loader.onSelect(descriptor -> {
                final TextWidget upload = new TextWidget(
                    "Selected " + descriptor.getName() + " 0%"
                );
                descriptor.getLoadingPercentageModel().addListener(
                    percent -> upload.setText(
                        "Selected " + descriptor.getName() + " " + percent + "%"
                    )
                );
                descriptor.onLoad(file -> upload.setText(
                    "Loaded " + file.getName() + " 100% " + sha256(file.getContent())
                ));
                content.add(upload);
            });
            content.add(status);
            content.add(button);
            content.add(loader);
            root.add(content);
        };

        final Options options = new Options.Builder()
            .setPort(Integer.parseInt(args[0]))
            .setBindAddress(InetAddress.getLoopbackAddress())
            .build();
        Server.start(new Application(page), options);
    }

    /**
     * Calculates a stable digest for browser-to-Java binary verification.
     *
     * @param data uploaded bytes
     * @return lowercase SHA-256 digest
     */
    private static String sha256(final byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (final NoSuchAlgorithmException error) {
            throw new AssertionError("SHA-256 is unavailable", error);
        }
    }
}
