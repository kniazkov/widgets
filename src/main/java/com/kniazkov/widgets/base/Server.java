/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.Handler;
import com.kniazkov.webserver.ServerException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
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

    /**
     * Prevents construction of the static entry-point class.
     */
    private Server() {
        /*
         * Static-only class
         */
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
        application.beginStart(options);

        /*
         * Start the underlying HTTP server
         */
        final com.kniazkov.webserver.Server server;
        try {
            server = com.kniazkov.webserver.Server.start(
                getWebServerOptions(options, handler)
            );
        } catch (final ServerException exception) {
            application.close();
            LOGGER.log(Level.SEVERE, "Unable to start the web server", exception);
            throw new IllegalStateException("Unable to start the web server", exception);
        } catch (final RuntimeException | Error failure) {
            application.close();
            throw failure;
        }

        /*
         * Log startup
         */
        LOGGER.info("Server started.");
        final ManagedServer managed = new ManagedServer(server, application);
        try {
            application.onClose(managed::stopAfterApplication);
        } catch (final IllegalStateException closed) {
            managed.stopAfterApplication();
            throw closed;
        }
        return managed;
    }

    /**
     * Keeps the existing public server interface while joining its lifecycle to the application.
     */
    private record ManagedServer(com.kniazkov.webserver.Server delegate,
            Application application, AtomicBoolean stopped)
            implements com.kniazkov.webserver.Server {
        /**
         * @param delegate transport listener
         * @param application owned application
         */
        ManagedServer(final com.kniazkov.webserver.Server delegate, final Application application) {
            this(delegate, application, new AtomicBoolean());
        }

        @Override
        public void stop() throws ServerException {
            if (this.stopped.compareAndSet(false, true)) {
                try {
                    this.application.close();
                } finally {
                    this.delegate.stop();
                }
            }
        }

        /**
         * Stops the listener after an independently closed application.
         */
        void stopAfterApplication() {
            try {
                this.stop();
            } catch (final ServerException failure) {
                throw new IllegalStateException("Unable to stop the web server", failure);
            }
        }

        @Override
        public int getPort() {
            return this.delegate.getPort();
        }

        @Override
        public InetAddress getBindAddress() {
            return this.delegate.getBindAddress();
        }
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
                .setMaxRequestSize(WebServerDefaults.uploadBudget(
                    WebServerDefaults.MAX_REQUEST_SIZE, source.getChunkSize()))
                .setMaxFileSize(WebServerDefaults.MAX_FILE_SIZE)
                .setMaxInMemoryBodySize(WebServerDefaults.MAX_IN_MEMORY_BODY_SIZE)
                .setMaxFormSize(WebServerDefaults.uploadBudget(
                    WebServerDefaults.MAX_FORM_SIZE, source.getChunkSize()))
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
