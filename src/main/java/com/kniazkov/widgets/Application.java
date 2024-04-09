/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
     * Constructor.
     */
    public Application() {
        this.clients = new TreeMap<>();

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
        final Client client = new Client();
        assert this.options.clientLifetime > 0;
        client.timer = this.options.clientLifetime;
        final UId id = client.getId();
        this.clients.put(id, client);
        return id;
    }

    /**
     * Watchdog that periodically walks through customers and bites them.
     */
    private class Watchdog extends Periodic {
        @Override
        protected boolean tick() {
            final Set<UId> destroy = new TreeSet<>();
            for (Map.Entry<UId, Client> entry: clients.entrySet()) {
                final Client client = entry.getValue();
                client.timer -= watchdogPeriod;
                if (client.timer <= 0) {
                    destroy.add(entry.getKey());
                }
            }
            for (final UId id : destroy) {
                clients.remove(id);
                options.logger.write("Client " + id.toString() + " destroyed.");
            }
            return true;
        }
    }
}
