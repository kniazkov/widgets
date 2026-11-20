/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.e2e;

import com.kniazkov.widgets.base.Application;
import com.kniazkov.widgets.base.Options;
import com.kniazkov.widgets.base.PushServer;
import com.kniazkov.widgets.base.Server;
import com.kniazkov.widgets.base.StreamOptions;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import com.kniazkov.widgets.view.TextWidget;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One independent process in the deployment probe. No session/controller replication.
 */
public final class DeploymentTestServer {
    /**
     * No instances.
     */
    private DeploymentTestServer() { }

    /**
     * @param args HTTP port, WS port, public origin, node label, SQLite file, metrics file
     */
    public static void main(final String[] args) {
        final DeploymentLedger ledger = new DeploymentLedger(args[4]);
        final AtomicInteger handlers = new AtomicInteger();
        final Application app = new Application((root, context) -> {
            final AtomicBoolean authorized = new AtomicBoolean();
            final TextWidget status = new TextWidget("Signed out");
            final TextWidget counter = new TextWidget("Counter requires login");
            final Button login = new Button("Demo login");
            login.onClick(event -> {
                authorized.set(true);
                status.setText("Signed in");
                counter.setText("Durable count: " + ledger.count());
            });
            final Section section = new Section(new TextWidget("Node " + args[3]), status,
                counter, login);
            for (int number = 1; number <= 2; number++) {
                final String key = "demo-account:operation-" + number;
                final Button command = new Button("Commit operation " + number);
                command.onClick(event -> {
                    if (!authorized.get()) {
                        return;
                    }
                    handlers.incrementAndGet();
                    ledger.apply(key);
                    counter.setText("Durable count: " + ledger.count());
                });
                section.add(command);
            }
            final Button logout = new Button("Logout");
            logout.onClick(event -> {
                authorized.set(false);
                context.browserSession.revoke();
            });
            section.add(logout);
            root.add(section);
        });
        Server.start(app, new Options.Builder().setPort(Integer.parseInt(args[0]))
            .setBindAddress(InetAddress.getLoopbackAddress()).setPublicOrigin(args[2]).build());
        final PushServer push = PushServer.start(app,
            StreamOptions.defaults(Integer.parseInt(args[1]), args[2]));
        final var journal = Executors.newSingleThreadScheduledExecutor();
        journal.scheduleWithFixedDelay(() -> {
            try {
                Files.writeString(Path.of(args[5]), "{\"handlers\":" + handlers.get()
                    + ",\"count\":" + ledger.count() + ",\"connections\":"
                    + push.getMetrics().get("connections") + "}");
            } catch (final java.io.IOException error) {
                throw new IllegalStateException(error);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        app.onClose(journal::shutdownNow);
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
    }
}
