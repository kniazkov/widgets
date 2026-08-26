/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.ErrorPage;
import com.kniazkov.webserver.SslOptions;
import java.net.InetAddress;
import java.time.Duration;

/**
 * Configuration options used when starting the server.
 */
public class Options implements Cloneable {
    /**
     * Maximum lifetime of a client without updates, in milliseconds.
     * If no requests are received from a client (i.e., the browser tab) within this time,
     * the client is considered disconnected and will be removed by the server watchdog.
     * This value should be long enough to account for tabs running in the background,
     * where JavaScript timers are throttled by the browser (often to 1 update per minute).
     * A default of 3 minutes provides a balance between connection reliability and
     * timely cleanup of zombie sessions.
     */
    public long clientLifetime = 3 * 60 * 1000;

    /**
     * Root directory for static files served via HTTP GET.
     * Any request that points to a file path (including direct access from a browser address bar)
     * is resolved relative to this directory. It should contain all project assets intended to be
     * served as static content—HTML, JavaScript, CSS, images, and other public resources.
     */
    public String wwwRoot = "www";

    /**
     * The HTTP or HTTPS port number on which the server will run.
     * A value of {@code 0} asks the operating system to select a free port.
     */
    public int port = 8080;

    /** Local address to bind, or {@code null} to listen on all local addresses. */
    public InetAddress bindAddress = null;

    /** Requested maximum length of the operating-system accept queue. */
    public int backlog = 50;

    /** Maximum complete HTTP request size, in bytes. */
    public long maxRequestSize = 128L * 1024L * 1024L;

    /** Maximum size of one uploaded file, in bytes. */
    public long maxFileSize = 128L * 1024L * 1024L;

    /** Maximum request body size retained in memory before temporary-file storage is used. */
    public long maxInMemoryBodySize = 64L * 1024L;

    /** Maximum decoded form-data size, in bytes. */
    public long maxFormSize = 1024L * 1024L;

    /** Maximum number of parts accepted in one multipart request. */
    public int maxMultipartParts = 1000;

    /** Maximum header size of one multipart part, in bytes. */
    public long maxMultipartHeaderSize = 16L * 1024L;

    /** Maximum HTTP request-line and header-section size, in bytes. */
    public long maxHeaderSize = 64L * 1024L;

    /** Maximum number of concurrently processed persistent connections. */
    public int maxWorkers = 100;

    /** Maximum wait for request data; preserves the framework's former five-second timeout. */
    public Duration readTimeout = Duration.ofSeconds(5);

    /** Maximum time allowed for writing and flushing one response. */
    public Duration writeTimeout = Duration.ofSeconds(30);

    /** Maximum request-handler execution time. */
    public Duration handlerTimeout = Duration.ofSeconds(30);

    /** Custom renderer for HTTP error responses, or {@code null} for the webserver default. */
    public ErrorPage errorPage = null;

    /**
     * HTTPS configuration, or {@code null} for plain HTTP.
     * The value can describe a PKCS #12/JKS identity, PEM identity, TLS policy, and mTLS trust.
     */
    public SslOptions sslOptions = null;

    /**
     * Outputs debug messages to the log on both the client and the server.
     */
    public boolean debug = true;

    @Override
    public Options clone() {
        Options copy = new Options();
        copy.clientLifetime = this.clientLifetime;
        copy.wwwRoot = this.wwwRoot;
        copy.port = this.port;
        copy.bindAddress = this.bindAddress;
        copy.backlog = this.backlog;
        copy.maxRequestSize = this.maxRequestSize;
        copy.maxFileSize = this.maxFileSize;
        copy.maxInMemoryBodySize = this.maxInMemoryBodySize;
        copy.maxFormSize = this.maxFormSize;
        copy.maxMultipartParts = this.maxMultipartParts;
        copy.maxMultipartHeaderSize = this.maxMultipartHeaderSize;
        copy.maxHeaderSize = this.maxHeaderSize;
        copy.maxWorkers = this.maxWorkers;
        copy.readTimeout = this.readTimeout;
        copy.writeTimeout = this.writeTimeout;
        copy.handlerTimeout = this.handlerTimeout;
        copy.errorPage = this.errorPage;
        copy.sslOptions = this.sslOptions;
        copy.debug = this.debug;
        return copy;
    }
}
