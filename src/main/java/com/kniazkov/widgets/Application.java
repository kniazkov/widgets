/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web application that is executed by the {@link Server}.
 * In order to use this library, you have to create your web application based on this class.
 */
public final class Application {
    /**
     * The interval of time in which a watchdog walks over clients and bites them.
     */
    private static final long watchdogPeriod = 100;

    /**
     * Options.
     */
    private Options options;

    /**
     * Set of clients.
     */
    private final Map<UId, Client> clients;

    /**
     * Set of pages.
     */
    private final Map<String, Page> pages;

    /**
     * Counter of processed actions.
     */
    private int counter;

    /**
     * Constructor.
     * @param index Index (main) page.
     */
    public Application(final @NotNull Page index) {
        this.clients = new ConcurrentHashMap<>();

        this.pages = new TreeMap<>();
        this.pages.put("/", index);

        this.counter = 0;

        Watchdog watchdog = new Watchdog();
        watchdog.start(watchdogPeriod);
    }

    /**
     * Sets options.
     * @param options Options
     */
    void setOptions(final @NotNull Options options) {
        this.options = options;
    }

    /**
     * Creates a new client instance.
     * @return Unique identifier of the created client
     */
    UId createClient() {
        counter++;
        final Client client = new Client();
        assert this.options.clientLifetime > 0;
        client.timer = this.options.clientLifetime;
        final UId id = client.getId();
        this.clients.put(id, client);
        final RootWidget root = client.getRootWidget();
        this.pages.get("/").create(root);
        return id;
    }

    /**
     * Kills the client (usually at the client's own request when the web page is closed).
     * @param clientId Client identified
     * @return Operation result, {@code true} if client was killed
     */
    boolean killClient(final @NotNull UId clientId) {
        counter++;
        final Client removed = this.clients.remove(clientId);
        return removed != null;
    }

    /**
     * Collects updates from all widgets of the specified client to send to the web page.
     * Also, it updates the timer so that another bite from the watchdog doesn't kill the client.
     * @param clientId Client identifier
     * @return List of instruction
     */
    @NotNull List<Instruction> getUpdates(final @NotNull UId clientId) {
        counter++;
        final Client client = this.clients.get(clientId);
        if (client == null) {
            return Collections.singletonList(new ResetClient());
        }
        client.timer = this.options.clientLifetime;
        return client.getRootWidget().collectUpdates();
    }

    /**
     * Handles event that was sent by web page.
     * @param clientId Client id
     * @param widgetId Widget id
     * @param type Event type
     * @param data Event-related data
     */
    void handleEvent(final UId clientId, final UId widgetId, final @NotNull String type,
                     final @Nullable JsonObject data) {
        counter++;
        final Client client = this.clients.get(clientId);
        if (client != null) {
            client.timer = this.options.clientLifetime;
            client.handleEvent(widgetId, type, data);
        }
    }

    /**
     * Watchdog that periodically walks through customers and bites them.
     */
    private class Watchdog extends Periodic {
        @Override
        protected boolean tick() {
            final Set<UId> kill = new TreeSet<>();
            for (Map.Entry<UId, Client> entry: clients.entrySet()) {
                final Client client = entry.getValue();
                client.timer -= watchdogPeriod;
                if (client.timer <= 0) {
                    kill.add(entry.getKey());
                }
            }
            for (final UId id : kill) {
                clients.remove(id);
                options.logger.write("Client " + id.toString() + " is killed by the watchdog.");
            }
            if (getTotalTime() % 60000 == 0) {
                if (counter > 0) {
                    options.logger.write(
                            "Server processed " + counter + " action" +
                                    (counter != 1 ? "s" : "") + " in one minute (~ " + (counter / 60) +
                                    " per second)."
                    );
                    counter = 0;
                } else {
                    options.logger.write("Server processed no actions.");
                }
            }
            return true;
        }
    }
}
