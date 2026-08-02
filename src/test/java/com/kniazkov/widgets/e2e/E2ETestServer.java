/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.common.UploadedFile;
import com.kniazkov.widgets.controller.UploadEvent;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

/** Test-only application used by the Playwright end-to-end suite. */
public final class E2ETestServer {
    private E2ETestServer() {
        // Static entry point only.
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
            final TextWidget uploadStatus = new TextWidget("Waiting for binary upload");
            final AtomicInteger chunks = new AtomicInteger();
            final FileLoader loader = new FileLoader("Upload binary file") {
                @Override
                public boolean handleUploadEvent(final UploadEvent event) {
                    chunks.incrementAndGet();
                    return super.handleUploadEvent(event);
                }
            };

            button.onClick(event -> status.setText("Java handled the click"));
            loader.onSelect(upload -> upload.onLoad(file -> uploadStatus.setText(
                uploadResult(file, chunks.get())
            )));
            content.add(status);
            content.add(button);
            content.add(uploadStatus);
            content.add(loader);
            root.add(content);
        };

        final Options options = new Options();
        options.port = Integer.parseInt(args[0]);
        Server.start(new Application(page), options);
    }

    /** Builds a deterministic result that lets Playwright verify bytes and request count. */
    private static String uploadResult(final UploadedFile file, final int chunks) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(file.getContent());
            final char[] hex = new char[digest.length * 2];
            final char[] alphabet = "0123456789abcdef".toCharArray();
            for (int index = 0; index < digest.length; index++) {
                final int value = digest[index] & 0xff;
                hex[index * 2] = alphabet[value >>> 4];
                hex[index * 2 + 1] = alphabet[value & 0x0f];
            }
            return "Uploaded " + file.getName() + ": " + file.getSize()
                + " bytes, sha256=" + new String(hex) + ", chunks=" + chunks;
        } catch (final NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
