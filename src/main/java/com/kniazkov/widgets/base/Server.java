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
     * @param options immutable application and listener configuration
     * @return the running web server, which can be stopped by the caller
     * @throws IllegalStateException if the HTTP or HTTPS listener cannot be started
     */
    public static com.kniazkov.webserver.Server start(
            final Application application, final Options options) {
        final Handler handler = new HttpHandler(application, options);
        application.setOptions(options);

        // Start the underlying HTTP server
        final com.kniazkov.webserver.Server server;
        try {
            server = com.kniazkov.webserver.Server.start(
                getWebServerOptions(options, handler)
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
                .setPort(source.getPort())
                .setBacklog(WebServerDefaults.BACKLOG)
                .setMaxRequestSize(WebServerDefaults.MAX_REQUEST_SIZE)
                .setMaxFileSize(WebServerDefaults.MAX_FILE_SIZE)
                .setMaxInMemoryBodySize(WebServerDefaults.MAX_IN_MEMORY_BODY_SIZE)
                .setMaxFormSize(WebServerDefaults.MAX_FORM_SIZE)
                .setMaxMultipartParts(WebServerDefaults.MAX_MULTIPART_PARTS)
                .setMaxMultipartHeaderSize(WebServerDefaults.MAX_MULTIPART_HEADER_SIZE)
                .setMaxHeaderSize(WebServerDefaults.MAX_HEADER_SIZE)
                .setMaxWorkers(source.getMaxWorkers())
                .setReadTimeout(WebServerDefaults.READ_TIMEOUT)
                .setWriteTimeout(WebServerDefaults.WRITE_TIMEOUT)
                .setHandlerTimeout(WebServerDefaults.HANDLER_TIMEOUT)
                .setHandler(handler);
        source.getBindAddress().ifPresent(builder::setBindAddress);
        source.getSslOptions().ifPresent(builder::setSslOptions);
        return builder.build();
    }
}
