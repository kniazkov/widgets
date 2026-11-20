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
     * Baseline complete request budget, including multipart framing.
     */
    static final long MAX_REQUEST_SIZE = 1024L * 1024L;

    /**
     * Maximum direct multipart file size; framework uploads use ordinary form chunks.
     */
    static final long MAX_FILE_SIZE = 1024L * 1024L;

    /**
     * Keeps one binary chunk and its multipart envelope in memory.
     */
    static final long MAX_IN_MEMORY_BODY_SIZE = 128L * 1024L;

    /**
     * Baseline decoded form-data size.
     */
    static final long MAX_FORM_SIZE = 1024L * 1024L;

    /**
     * Bounded envelope allowance for the six fixed fields and one binary part. This is larger
     * than seven maximum 8 KiB part headers plus ordinary browser boundaries and scalar values.
     */
    static final long UPLOAD_ENVELOPE_SIZE = 64L * 1024L;

    /**
     * Derives a bounded HTTP/form budget with room for a full-sized configured upload chunk.
     *
     * @param baseline normal listener limit
     * @param chunkSize configured binary payload ceiling, validated by Options
     * @return total listener limit, not the binary part limit
     */
    static long uploadBudget(final long baseline, final int chunkSize) {
        return Math.max(baseline, chunkSize + UPLOAD_ENVELOPE_SIZE);
    }

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
