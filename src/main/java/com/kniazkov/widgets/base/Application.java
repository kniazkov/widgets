/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.json.JsonObject;
import com.kniazkov.widgets.common.RMId;
import com.kniazkov.widgets.view.RootWidget;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Web application executed by the {@link Server}.
 * To use this framework, you must implement and provide at least one {@link Page}
 * (the index page) and pass it to this class.
 */
public final class Application {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Application.class.getName());


    /**
     * Interval at which the internal watchdog checks all clients, in milliseconds.
     */
    private static final long WATCHDOG_PERIOD = 100;

    /**
     * Application options (e.g., logger, client timeout).
     */
    private Options options;

    /**
     * All active clients, keyed by unique identifier.
     */
    private final ConcurrentMap<RMId, Client> clients;

    /**
     * Available pages in this application, keyed by path.
     */
    private final Map<String, Page> pages;

    /**
     * Counter of processed actions since last watchdog report.
     */
    private int counter;

    /**
     * Constructs an application with a given index page (served at path {@code "/"}).
     *
     * @param index the root page of the application
     */
    public Application(Page index) {
        this.clients = new ConcurrentHashMap<>();
        this.pages = new TreeMap<>();
        this.pages.put("/", index);
        this.counter = 0;

        // Start watchdog to clean up inactive clients
        Watchdog watchdog = new Watchdog();
        watchdog.start(WATCHDOG_PERIOD);
    }

    /**
     * Registers an additional page in the application under the given address.
     * <p>
     * The resulting page becomes available at the URL:
     * <pre>
     *   http://&lt;host&gt;/address
     * </pre>
     * For example, calling {@code addPage("name", page)} makes the page accessible at
     * <code>http://localhost:8000/name</code>.
     *
     * @param address the URL path segment without the leading slash
     * @param page    the page instance to register
     */
    public void addPage(final String address, final Page page) {
        this.pages.put("/" + address, page);
    }

    /**
     * Checks whether a page is registered under the specified address.
     *
     * @param address the full URL path (including the leading slash)
     * @return {@code true} if a page exists for that address, otherwise {@code false}
     */
    boolean hasPage(final String address) {
        return this.pages.containsKey(address);
    }

    /**
     * Sets configuration options for this application.
     *
     * @param options application options
     */
    void setOptions(Options options) {
        this.options = options;
    }

    /**
     * Creates a new client and initializes its page.
     *
     * @param address page address
     * @param context container for request-specific settings passed to a page
     * @return the unique identifier of the created client
     */
    RMId createClient(final String address, final PageContext context) {
        this.counter++;
        final Client client = new Client();
        client.timer = this.options.getClientLifetime();

        final RMId id = client.getId();
        final RootWidget root = client.getRootWidget();
        final Page page = this.pages.get(this.pages.containsKey(address) ? address : "/");
        try {
            page.create(root, context);
        } catch (final RuntimeException | Error failure) {
            client.destroy();
            throw failure;
        }

        this.clients.put(id, client);
        return id;
    }

    /**
     * Terminates a client and removes it from memory.
     * Typically called when the browser tab is closed.
     *
     * @param clientId the client to kill
     * @return {@code true} if the client was removed
     */
    boolean killClient(final RMId clientId) {
        this.counter++;
        final Client client = this.clients.remove(clientId);
        if (client != null) {
            synchronized (client) {
                client.destroy();
            }
            return true;
        }
        return false;
    }


    /**
     * Handles a synchronization request for a specific client.
     * <p>
     *     This method is invoked when a web page requests to synchronize its state
     *     with the server. It performs the following operations:
     * </p>
     * <ul>
     *     <li>Increments the internal action counter.</li>
     *     <li>Finds the client instance by its ID.</li>
     *     <li>
     *         Resets the client's lifetime timer to avoid premature termination
     *         by the watchdog.
     *     </li>
     *     <li>Delegates the actual processing of events and updates to the client.</li>
     * </ul>
     *
     * @param clientId The unique identifier of the client session
     * @param request  The incoming request parameters from the client (e.g. events,
     *                        lastUpdate)
     * @param response The JSON object to be populated with UI update instructions and state
     */
    void synchronize(final RMId clientId, final Map<String, String> request,
                        final JsonObject response) {
        this.counter++;
        this.clients.computeIfPresent(clientId, (id, client) -> {
            synchronized (client) {
                client.timer = this.options.getClientLifetime();
                client.synchronize(request, response);
                response.addBoolean("result", true);
            }
            return client;
        });
    }

    /**
     * Watchdog that periodically walks through clients and removes stale ones.
     * Also logs performance stats every minute.
     */
    private class Watchdog extends Periodic {
        @Override
        protected boolean tick() {
            for (final Map.Entry<RMId, Client> entry : clients.entrySet()) {
                final RMId id = entry.getKey();
                clients.computeIfPresent(id, (key, client) -> {
                    synchronized (client) {
                        client.timer -= WATCHDOG_PERIOD;
                        if (client.timer <= 0) {
                            client.destroy();
                            LOGGER.info("Client " + id + " is killed by the watchdog.");
                            return null;
                        }
                    }
                    return client;
                });
            }

            // Every minute, log performance
            if (this.getTotalTime() % 60000 == 0) {
                if (counter > 0) {
                    LOGGER.info("Server processed " + counter + " action"
                        + (counter != 1 ? "s" : "") + " in one minute (~" + (counter / 60) + "/sec).");
                    counter = 0;
                } else {
                    LOGGER.info("Server processed no actions.");
                }
            }

            return true; // Continue ticking
        }
    }
}
