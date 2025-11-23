/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.Handler;
import java.util.logging.Logger;


/**
 * The entry point for running a web application.
 * This class starts the HTTP server and binds the provided application to it.
 * It acts as a bridge between the widget-based UI framework and the low-level HTTP layer.
 */
public final class Server {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private Server() {
        // Static-only class
    }

    /**
     * Starts the web server and runs the given application.
     *
     * @param application the application instance to launch
     * @param options configuration options (logger, timeouts, etc.)
     */
    public static void start(final Application application, final Options options) {
        // Clone options so the application can modify them safely
        final Options cloned = options.clone();
        final Handler handler = new HttpHandler(application, cloned);
        application.setOptions(cloned);

        // Start the underlying HTTP server
        com.kniazkov.webserver.Server.start(getWebServerOptions(cloned), handler);

        // Log startup
        LOGGER.info("Server started.");
    }

    /**
     * Returns configuration options for the underlying web server.
     * This method may be extended in the future.
     *
     * @param o1 configuration options for widget application
     * @return web server options
     */
    private static com.kniazkov.webserver.Options getWebServerOptions(Options o1) {
        final com.kniazkov.webserver.Options o2 = new com.kniazkov.webserver.Options();
        o2.port = o1.port;
        o2.timeout = 5000;
        return o2;
    }
}
