/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.ServerException;
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
     * @return the running web server, which can be stopped by the caller
     * @throws IllegalStateException if the HTTP or HTTPS listener cannot be started
     */
    public static com.kniazkov.webserver.Server start(
            final Application application, final Options options) {
        // Clone options so the application can modify them safely
        final Options cloned = options.clone();
        final Handler handler = new HttpHandler(application, cloned);
        application.setOptions(cloned);

        // Start the underlying HTTP server
        final com.kniazkov.webserver.Server server;
        try {
            server = com.kniazkov.webserver.Server.start(
                getWebServerOptions(cloned, handler)
            );
        } catch (final ServerException exception) {
            throw new IllegalStateException("Unable to start the web server", exception);
        }

        // Log startup
        LOGGER.info("Server started.");
        return server;
    }

    /**
     * Returns configuration options for the underlying web server.
     * This method may be extended in the future.
     *
     * @param source configuration options for widget application
     * @param handler framework HTTP request handler
     * @return web server options
     */
    static com.kniazkov.webserver.Options getWebServerOptions(
            final Options source, final Handler handler) {
        final com.kniazkov.webserver.Options.Builder builder =
            new com.kniazkov.webserver.Options.Builder()
                .setPort(source.port)
                .setBacklog(source.backlog)
                .setWwwRoot(source.wwwRoot)
                .setMaxRequestSize(source.maxRequestSize)
                .setMaxFileSize(source.maxFileSize)
                .setMaxInMemoryBodySize(source.maxInMemoryBodySize)
                .setMaxFormSize(source.maxFormSize)
                .setMaxMultipartParts(source.maxMultipartParts)
                .setMaxMultipartHeaderSize(source.maxMultipartHeaderSize)
                .setMaxHeaderSize(source.maxHeaderSize)
                .setMaxWorkers(source.maxWorkers)
                .setReadTimeout(source.readTimeout)
                .setWriteTimeout(source.writeTimeout)
                .setHandlerTimeout(source.handlerTimeout)
                .setHandler(handler);
        if (source.bindAddress != null) {
            builder.setBindAddress(source.bindAddress);
        }
        if (source.errorPage != null) {
            builder.setErrorPage(source.errorPage);
        }
        if (source.sslOptions != null) {
            builder.setSslOptions(source.sslOptions);
        }
        return builder.build();
    }
}
