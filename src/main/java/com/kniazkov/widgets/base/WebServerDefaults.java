/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.time.Duration;

/**
 * Internal HTTP profile for the framework's short XMLHttpRequest exchanges.
 */
final class WebServerDefaults {
    /**
     * Requested operating-system accept queue size.
     */
    static final int BACKLOG = 50;

    /**
     * Maximum direct legacy multipart file size.
     */
    static final long MAX_FILE_SIZE = 1024L * 1024L;

    /**
     * Maximum complete request size for a hex-encoded maximum-size chunk and framing.
     */
    static final long MAX_REQUEST_SIZE = MAX_FILE_SIZE * 2L + 64L * 1024L;

    /**
     * Keeps small request bodies in memory; larger encoded chunks use temporary storage.
     */
    static final long MAX_IN_MEMORY_BODY_SIZE = 128L * 1024L;

    /**
     * Maximum decoded form-data size.
     */
    static final long MAX_FORM_SIZE = MAX_REQUEST_SIZE;

    /**
     * Maximum number of fields in one multipart XMLHttpRequest.
     */
    static final int MAX_MULTIPART_PARTS = 64;

    /**
     * Maximum header size for one multipart field.
     */
    static final long MAX_MULTIPART_HEADER_SIZE = 8L * 1024L;

    /**
     * Maximum HTTP request-line and header-section size.
     */
    static final long MAX_HEADER_SIZE = 32L * 1024L;

    /**
     * Maximum wait for request data.
     */
    static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Maximum time allowed for writing and flushing one response.
     */
    static final Duration WRITE_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Maximum request-handler execution time.
     */
    static final Duration HANDLER_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Prevents construction of the constants-only class.
     */
    private WebServerDefaults() {
        /*
         * Constants only
         */
    }
}
