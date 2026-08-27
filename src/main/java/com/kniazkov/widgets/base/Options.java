/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.SslOptions;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable configuration used when starting a widget application.
 */
public final class Options {
    /**
     * Default maximum lifetime of an inactive browser client, in milliseconds.
     */
    private static final long DEFAULT_CLIENT_LIFETIME = 3L * 60L * 1000L;

    /**
     * Default root directory for public application files.
     */
    private static final String DEFAULT_WWW_ROOT = "www";

    /**
     * Default HTTP port.
     */
    private static final int DEFAULT_PORT = 8080;

    /**
     * Default maximum number of concurrently processed connections.
     */
    private static final int DEFAULT_MAX_WORKERS = 100;

    /**
     * Default binary upload chunk size.
     */
    private static final int DEFAULT_CHUNK_SIZE = 64 * 1024;

    /**
     * Default maximum size of one complete uploaded file.
     */
    private static final int DEFAULT_MAX_FILE_SIZE = 128 * 1024 * 1024;

    /**
     * Maximum lifetime of an inactive browser client, in milliseconds.
     */
    private final long clientLifetime;

    /**
     * Root directory for public application files.
     */
    private final String wwwRoot;

    /**
     * HTTP or HTTPS listener port.
     */
    private final int port;

    /**
     * Local listener address, or {@code null} to bind to every local address.
     */
    private final InetAddress bindAddress;

    /**
     * Maximum number of concurrently processed connections.
     */
    private final int maxWorkers;

    /**
     * HTTPS configuration, or {@code null} for plain HTTP.
     */
    private final SslOptions sslOptions;

    /**
     * Binary upload chunk size.
     */
    private final int chunkSize;

    /**
     * Maximum size of one complete uploaded file.
     */
    private final int maxFileSize;

    /**
     * Whether browser and server debug logging is enabled.
     */
    private final boolean debug;

    /**
     * Creates immutable options from a builder snapshot.
     *
     * @param builder source builder
     */
    private Options(final Builder builder) {
        this.clientLifetime = builder.clientLifetime;
        this.wwwRoot = builder.wwwRoot;
        this.port = builder.port;
        this.bindAddress = builder.bindAddress;
        this.maxWorkers = builder.maxWorkers;
        this.sslOptions = builder.sslOptions;
        this.chunkSize = builder.chunkSize;
        this.maxFileSize = builder.maxFileSize;
        this.debug = builder.debug;
    }

    /**
     * Returns the maximum lifetime of an inactive browser client.
     *
     * @return lifetime in milliseconds
     */
    public long getClientLifetime() {
        return this.clientLifetime;
    }

    /**
     * Returns the root directory for public application files.
     *
     * @return public-file root
     */
    public String getWwwRoot() {
        return this.wwwRoot;
    }

    /**
     * Returns the HTTP or HTTPS listener port.
     *
     * @return configured port; {@code 0} requests an automatically selected port
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Returns the local listener address.
     *
     * @return configured address, or an empty optional to bind to every local address
     */
    public Optional<InetAddress> getBindAddress() {
        return Optional.ofNullable(this.bindAddress);
    }

    /**
     * Returns the maximum number of concurrently processed connections.
     *
     * @return worker limit
     */
    public int getMaxWorkers() {
        return this.maxWorkers;
    }

    /**
     * Returns the HTTPS configuration.
     *
     * @return SSL options, or an empty optional for plain HTTP
     */
    public Optional<SslOptions> getSslOptions() {
        return Optional.ofNullable(this.sslOptions);
    }

    /**
     * Returns the binary upload chunk size.
     *
     * @return chunk size in bytes
     */
    public int getChunkSize() {
        return this.chunkSize;
    }

    /**
     * Returns the maximum size of one complete uploaded file.
     *
     * @return file size limit in bytes
     */
    public int getMaxFileSize() {
        return this.maxFileSize;
    }

    /**
     * Returns whether browser and server debug logging is enabled.
     *
     * @return debug flag
     */
    public boolean isDebug() {
        return this.debug;
    }

    /**
     * Builds immutable application options.
     */
    public static final class Builder {
        /**
         * Maximum lifetime of an inactive browser client, in milliseconds.
         */
        private long clientLifetime = DEFAULT_CLIENT_LIFETIME;

        /**
         * Root directory for public application files.
         */
        private String wwwRoot = DEFAULT_WWW_ROOT;

        /**
         * HTTP or HTTPS listener port.
         */
        private int port = DEFAULT_PORT;

        /**
         * Local listener address.
         */
        private InetAddress bindAddress;

        /**
         * Maximum number of concurrently processed connections.
         */
        private int maxWorkers = DEFAULT_MAX_WORKERS;

        /**
         * HTTPS configuration.
         */
        private SslOptions sslOptions;

        /**
         * Binary upload chunk size.
         */
        private int chunkSize = DEFAULT_CHUNK_SIZE;

        /**
         * Maximum size of one complete uploaded file.
         */
        private int maxFileSize = DEFAULT_MAX_FILE_SIZE;

        /**
         * Whether browser and server debug logging is enabled.
         */
        private boolean debug = true;

        /**
         * Creates a builder initialized with framework defaults.
         */
        public Builder() {
        }

        /**
         * Sets the maximum lifetime of an inactive browser client.
         *
         * @param value lifetime in milliseconds
         * @return this builder
         */
        public Builder setClientLifetime(final long value) {
            if (value < 1) {
                throw new IllegalArgumentException("Client lifetime must be positive");
            }
            this.clientLifetime = value;
            return this;
        }

        /**
         * Sets the root directory for public application files.
         *
         * @param value public-file root
         * @return this builder
         */
        public Builder setWwwRoot(final String value) {
            Objects.requireNonNull(value, "WWW root must not be null");
            if (value.isBlank()) {
                throw new IllegalArgumentException("WWW root must not be empty");
            }
            this.wwwRoot = value;
            return this;
        }

        /**
         * Sets the HTTP or HTTPS listener port.
         *
         * @param value port in the range {@code 0..65535}
         * @return this builder
         */
        public Builder setPort(final int value) {
            if (value < 0 || value > 65535) {
                throw new IllegalArgumentException("Port must be between 0 and 65535");
            }
            this.port = value;
            return this;
        }

        /**
         * Sets the local listener address.
         *
         * @param value address to bind
         * @return this builder
         */
        public Builder setBindAddress(final InetAddress value) {
            this.bindAddress = Objects.requireNonNull(
                value,
                "Bind address must not be null"
            );
            return this;
        }

        /**
         * Sets the maximum number of concurrently processed connections.
         *
         * @param value positive worker limit
         * @return this builder
         */
        public Builder setMaxWorkers(final int value) {
            if (value < 1) {
                throw new IllegalArgumentException("Maximum worker count must be positive");
            }
            this.maxWorkers = value;
            return this;
        }

        /**
         * Enables HTTPS using the supplied immutable configuration.
         *
         * @param value SSL/TLS configuration
         * @return this builder
         */
        public Builder setSslOptions(final SslOptions value) {
            this.sslOptions = Objects.requireNonNull(
                value,
                "SSL options must not be null"
            );
            return this;
        }

        /**
         * Sets the binary upload chunk size.
         *
         * @param value positive chunk size in bytes
         * @return this builder
         */
        public Builder setChunkSize(final int value) {
            if (value < 1) {
                throw new IllegalArgumentException("Upload chunk size must be positive");
            }
            if (value > WebServerDefaults.MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                    "Upload chunk size exceeds the HTTP multipart limit"
                );
            }
            this.chunkSize = value;
            return this;
        }

        /**
         * Sets the maximum size of one complete uploaded file.
         *
         * @param value non-negative file size limit in bytes
         * @return this builder
         */
        public Builder setMaxFileSize(final int value) {
            if (value < 0) {
                throw new IllegalArgumentException("Maximum upload file size must not be negative");
            }
            this.maxFileSize = value;
            return this;
        }

        /**
         * Enables or disables browser and server debug logging.
         *
         * @param value debug flag
         * @return this builder
         */
        public Builder setDebug(final boolean value) {
            this.debug = value;
            return this;
        }

        /**
         * Builds an immutable snapshot of the current values.
         *
         * @return application options
         */
        public Options build() {
            return new Options(this);
        }
    }
}
