/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.widgets.controller.EventTestData;
import com.kniazkov.widgets.e2e.DeploymentLedger;
import com.kniazkov.widgets.view.Button;
import com.kniazkov.widgets.view.Section;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Separate application owners must never execute each other's authenticated UI commands.
 */
public final class DeploymentModelTest {
    /**
     * A valid same-origin request routed to a foreign process is explicitly rejected.
     */
    @Test
    public void wrongOwnerCannotExecuteOrAdvanceTheOtherChannel() throws Exception {
        final AtomicInteger first = new AtomicInteger();
        final AtomicInteger second = new AtomicInteger();
        final Button[] buttons = new Button[2];
        try (Application left = new Application((root, context) -> {
            buttons[0] = new Button("Left");
            buttons[0].onClick(event -> first.incrementAndGet());
            root.add(new Section(buttons[0]));
        }); Application right = new Application((root, context) -> {
            buttons[1] = new Button("Right");
            buttons[1].onClick(event -> second.incrementAndGet());
            root.add(new Section(buttons[1]));
        })) {
            final int leftPort = Server.start(left, new Options.Builder().setPort(0).build())
                .getPort();
            final int rightPort = Server.start(right, new Options.Builder().setPort(0).build())
                .getPort();
            final ProtocolTestBrowser a = new ProtocolTestBrowser(leftPort);
            final ProtocolTestBrowser b = new ProtocolTestBrowser(rightPort);
            final var channelA = a.create(null);
            final var channelB = b.create(null);
            final Map<String, String> command = Map.of("action", "synchronize", "client",
                channelA.get("id").stringValue(), "lastUpdate", "#0", "events",
                "[{\"id\":\"#1\",\"widget\":\"" + buttons[0].getId()
                    + "\",\"type\":\"click\",\"data\":" + EventTestData.pointer() + "}]");
            final var wrongRoute = new HashMap<>(a.headers(channelA));
            wrongRoute.put("Origin", b.uri.toString().replaceAll("/$", ""));
            assertEquals(403, b.post(command, wrongRoute).statusCode());
            assertEquals(0, first.get());
            assertEquals(0, second.get());
            assertEquals(200, a.post(command, a.headers(channelA)).statusCode());
            assertEquals(200, a.post(command, a.headers(channelA)).statusCode());
            assertEquals(1, first.get());
            final var foreignEpoch = new HashMap<>(b.headers(channelB));
            foreignEpoch.put("X-Widgets-Epoch", channelA.get("serverId").stringValue());
            final var otherCommand = new HashMap<>(command);
            otherCommand.put("client", channelB.get("id").stringValue());
            assertEquals(403, b.post(otherCommand, foreignEpoch).statusCode());
            assertEquals(0, second.get());
            left.browserSession(a.cookie.substring(a.cookie.indexOf('=') + 1)).revoke();
            assertEquals(403, a.post(command, a.headers(channelA)).statusCode());
            assertEquals(1, first.get());
        }
    }

    /**
     * Business idempotency belongs to a transaction in durable storage, not a widget event ID.
     */
    @Test
    public void durableBusinessKeySurvivesIndependentOwnersAndReopening() throws Exception {
        final Path path = Files.createTempFile("widgets-deployment-", ".sqlite");
        try {
            final DeploymentLedger first = new DeploymentLedger(path.toString());
            final DeploymentLedger second = new DeploymentLedger(path.toString());
            try (var workers = Executors.newFixedThreadPool(2)) {
                final var left = workers.submit(() -> first.apply("account-42:operation-1"));
                final var right = workers.submit(() -> second.apply("account-42:operation-1"));
                assertTrue(left.get() ^ right.get());
            }
            final DeploymentLedger restarted = new DeploymentLedger(path.toString());
            assertEquals(1, restarted.count());
            assertFalse(restarted.apply("account-42:operation-1"));
            assertTrue(restarted.apply("account-42:operation-2"));
            assertEquals(2, first.count());
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
