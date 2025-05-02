/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import com.kniazkov.webserver.Handler;

/**
 * The entry point for running a web application.
 * <p>
 *     This class starts the HTTP server and binds the provided application to it.
 *     It acts as a bridge between the widget-based UI framework and the low-level HTTP layer.
 * </p>
 */
public final class Server {

    private Server() {
        // Static-only class
    }

    /**
     * Starts the web server and runs the given application.
     *
     * @param application The application instance to launch
     * @param options Configuration options (logger, timeouts, etc.)
     */
    public static void start(final Application application, final Options options) {
        // Clone options so the application can modify them safely
        final Options cloned = options.clone();
        final Handler handler = new HttpHandler(application, cloned);
        application.setOptions(cloned);

        // Start the underlying HTTP server
        com.kniazkov.webserver.Server.start(getWebServerOptions(), handler);

        // Log startup
        options.logger.write("Server started.");
    }

    /**
     * Returns configuration options for the underlying web server.
     * This method may be extended in the future.
     *
     * @return Default web server options
     */
    private static com.kniazkov.webserver.Options getWebServerOptions() {
        return new com.kniazkov.webserver.Options();
    }
}
