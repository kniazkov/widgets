/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.SslOptions;
import com.kniazkov.widgets.common.WebFont;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable configuration used when starting a widget application.
 */
public final class Options {
    /**
     * Browser tab title.
     */
    private final String title;

    /**
     * Public favicon URL.
     */
    private final String faviconUrl;

    /**
     * Search result description.
     */
    private final String description;

    /**
     * Crawler indexing directives.
     */
    private final String robots;

    /**
     * Document language tag.
     */
    private final String language;

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
     * Fonts whose stylesheets are connected to every application page.
     */
    private final List<WebFont> webFonts;

    /**
     * Explicit public static resource mounts.
     */
    private final List<StaticSource> staticSources;

    /**
     * Creates immutable options from a builder snapshot.
     *
     * @param builder source builder
     */
    private Options(final Builder builder) {
        this.title = builder.title;
        this.faviconUrl = builder.faviconUrl;
        this.description = builder.description;
        this.robots = builder.robots;
        this.language = builder.language;
        this.clientLifetime = builder.clientLifetime;
        this.wwwRoot = builder.wwwRoot;
        this.port = builder.port;
        this.bindAddress = builder.bindAddress;
        this.maxWorkers = builder.maxWorkers;
        this.sslOptions = builder.sslOptions;
        this.chunkSize = builder.chunkSize;
        this.maxFileSize = builder.maxFileSize;
        this.debug = builder.debug;
        this.webFonts = List.copyOf(builder.webFonts);
        this.staticSources = List.copyOf(builder.staticSources);
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
     * Returns fonts connected to every application page.
     *
     * @return immutable font list in registration order
     */
    public List<WebFont> getWebFonts() {
        return this.webFonts;
    }

    /**
     * Returns the registered static mounts.
     *
     * @return immutable list in registration order
     */
    public List<StaticSource> getStaticSources() {
        return this.staticSources;
    }

    /**
     * Returns the browser tab title.
     *
     * @return configured value; empty means omitted for optional head elements
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Returns the public favicon url.
     *
     * @return configured value; empty means omitted for optional head elements
     */
    public String getFaviconUrl() {
        return this.faviconUrl;
    }

    /**
     * Returns the search result description.
     *
     * @return configured value; empty means omitted for optional head elements
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the crawler indexing directives.
     *
     * @return configured value; empty means omitted for optional head elements
     */
    public String getRobots() {
        return this.robots;
    }

    /**
     * Returns the document language tag.
     *
     * @return configured value; empty means omitted for optional head elements
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * Builds immutable application options.
     */
    public static final class Builder {
        /**
         * Browser tab title.
         */
        private String title = "";

        /**
         * Public favicon URL.
         */
        private String faviconUrl = "";

        /**
         * Search result description.
         */
        private String description = "";

        /**
         * Crawler indexing directives.
         */
        private String robots = "";

        /**
         * Document language tag.
         */
        private String language = "en";

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
         * Fonts to connect to every application page.
         */
        private final List<WebFont> webFonts = new ArrayList<>();

        /**
         * Public static resource mounts.
         */
        private final List<StaticSource> staticSources = new ArrayList<>();

        /**
         * Sets the browser tab title on every application page.
         * Empty text omits the element; null is rejected.
         *
         * @param value configuration value
         * @return this builder
         */
        public Builder setTitle(final String value) {
            Objects.requireNonNull(value, "Title must not be null");
            this.title = value;
            return this;
        }

        /**
         * Sets the public favicon url on every application page.
         * Use a root-relative path or an absolute HTTP(S) URL. Empty text omits the icon.
         *
         * @param value configuration value
         * @return this builder
         */
        public Builder setFaviconUrl(final String value) {
            Objects.requireNonNull(value, "FaviconUrl must not be null");
            if (!value.isEmpty()) {
                final URI uri = URI.create(value);
                final boolean local = value.startsWith("/") && !value.startsWith("//")
                    && uri.getRawAuthority() == null;
                final boolean remote = ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
                if ((!local && !remote) || value.contains("\\")) {
                    throw new IllegalArgumentException(
                        "Favicon must be a public HTTP(S) URL or path"
                    );
                }
            }
            this.faviconUrl = value;
            return this;
        }

        /**
         * Sets the search result description on every application page.
         * Empty text omits the element; null is rejected.
         *
         * @param value configuration value
         * @return this builder
         */
        public Builder setDescription(final String value) {
            Objects.requireNonNull(value, "Description must not be null");
            this.description = value;
            return this;
        }

        /**
         * Sets the crawler indexing directives on every application page.
         * Empty text omits the element; null is rejected.
         *
         * @param value configuration value
         * @return this builder
         */
        public Builder setRobots(final String value) {
            Objects.requireNonNull(value, "Robots must not be null");
            this.robots = value;
            return this;
        }

        /**
         * Sets the document language tag on every application page.
         * Use a language tag such as en or ru-RU; the default is en.
         *
         * @param value configuration value
         * @return this builder
         */
        public Builder setLanguage(final String value) {
            Objects.requireNonNull(value, "Language must not be null");
            if (!value.matches("[a-zA-Z]{2,8}(-[a-zA-Z0-9]{1,8})*")) {
                throw new IllegalArgumentException("Invalid document language tag");
            }
            this.language = value;
            return this;
        }

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
         * Adds a browser font whose stylesheet will be connected at startup.
         *
         * @param value external font configuration
         * @return this builder
         */
        public Builder addFont(final WebFont value) {
            this.webFonts.add(Objects.requireNonNull(
                value,
                "Web font must not be null"
            ));
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

        /**
         * Registers a public static subtree. Longest matching prefix wins; a missing file
         * never falls back to another mount or wwwRoot. Framework resources and pages win.
         *
         * @param source explicit directory or classpath source
         * @return this builder
         */
        public Builder addStaticSource(final StaticSource source) {
            Objects.requireNonNull(source, "source");
            for (final StaticSource existing : this.staticSources) {
                if (existing.getPrefix().equals(source.getPrefix())) {
                    throw new IllegalArgumentException("Duplicate static URL prefix");
                }
            }
            this.staticSources.add(source);
            return this;
        }
    }
}
