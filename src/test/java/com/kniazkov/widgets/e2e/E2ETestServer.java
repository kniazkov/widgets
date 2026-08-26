/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.Page;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.net.InetAddress;

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

            button.onClick(event -> status.setText("Java handled the click"));
            content.add(status);
            content.add(button);
            root.add(content);
        };

        final Options options = new Options.Builder()
            .setPort(Integer.parseInt(args[0]))
            .setBindAddress(InetAddress.getLoopbackAddress())
            .build();
        Server.start(new Application(page), options);
    }
}
