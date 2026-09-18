/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Carousel;
import com.kniazkov.widgets.view.FileLoader;
import com.kniazkov.widgets.view.InputField;
import com.kniazkov.widgets.view.Link;
import com.kniazkov.widgets.view.MessagePopup;
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
            final Button popupLauncher = new Button("Show modal message");
            final Carousel carousel = new Carousel(
                "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' "
                    + "width='300' height='180'%3E%3Crect width='100%25' height='100%25' "
                    + "fill='red'/%3E%3C/svg%3E",
                "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' "
                    + "width='300' height='180'%3E%3Crect width='100%25' height='100%25' "
                    + "fill='blue'/%3E%3C/svg%3E"
            );
            carousel.setWidth(300);
            carousel.setHeight(180);
            loader.setMultipleInputFlag(true);

            button.onClick(event -> status.setText("Java handled the click"));
            popupLauncher.onClick(event -> {
                final Button cancel = new Button("Cancel");
                final Button close = new Button("Close modal message");
                final MessagePopup popup = new MessagePopup(
                    "Modal message", cancel, close
                );
                cancel.onClick(cancelEvent -> popup.remove());
                close.onClick(closeEvent -> popup.remove());
                root.add(popup);
            });
            carousel.onSelect(index -> status.setText("Carousel selected " + index));
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
            content.add(popupLauncher);
            content.add(carousel);
            root.add(content);
        };

        final Options options = new Options.Builder()
            .setPort(Integer.parseInt(args[0]))
            .setBindAddress(InetAddress.getLoopbackAddress())
            .build();
        final Application application = new Application(page);
        application.addPage("catalog", (root, context) -> {
            final Section header = new Section();
            header.add(new InputField());
            final Link product = new Link("Open product");
            product.setHref("/product?id=42");
            header.add(product);
            final Button navigate = new Button("Navigate from Java");
            navigate.onClick(event -> root.goToPage("/product?id=43"));
            header.add(navigate);
            root.add(header);
            for (int index = 0; index < 80; index++) {
                final Section row = new Section();
                row.setTopPadding(20);
                row.setBottomPadding(20);
                row.add(new TextWidget("Catalog row " + index));
                root.add(row);
            }
        });
        application.addPage("product", (root, context) -> {
            final Section content = new Section();
            content.add(new TextWidget("Product " + context.parameters.get("id")));
            final Button clear = new Button("Clear cached pages");
            clear.onClick(event -> root.clearPageCache());
            content.add(clear);
            root.add(content);
        });
        Server.start(application, options);
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
