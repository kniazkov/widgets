/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.Request;
import java.util.List;
import java.util.Map;

/**
 * Bounded request metadata without bodies, query strings or form values.
 */
final class RequestDiagnostics {
    /**
     * No instances.
     */
    private RequestDiagnostics() {
    }

    /**
     * Describes a failed request without allowing diagnostics to replace the original failure.
     *
     * @param request request being handled
     * @return single-line diagnostic metadata
     */
    static String describe(final Request request) {
        try {
            final Map<String, List<String>> form = request.getForm();
            final String contentType = header(request, "Content-Type");
            return "method=" + request.getHeaders().getMethod()
                + " path=" + safe(request.getPath().getPath())
                + " contentType=" + safe(contentType.split(";", 2)[0])
                + " contentLength=" + safe(header(request, "Content-Length"))
                + " bodyBytes=" + request.getBody().getSize()
                + " formFields=" + form.size()
                + " fileFields=" + request.getFiles().size()
                + " formAction=" + form.containsKey("action")
                + " queryAction=" + request.getQuery().containsKey("action")
                + " clientField=" + form.containsKey("client")
                + " browserIdField=" + form.containsKey("browserId")
                + " userAgent=" + safe(header(request, "User-Agent"));
        } catch (final RuntimeException failure) {
            /*
             * Metadata is best-effort, including when the request itself is invalid.
             * The enclosing boundary must still log and rethrow the original exception.
             */
            return "requestMetadata=unavailable";
        }
    }

    /**
     * Reads the first header value independently of header-name casing.
     */
    private static String header(final Request request, final String name) {
        for (final Map.Entry<String, List<String>> entry
                : request.getHeaders().getValues().entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey()) && !entry.getValue().isEmpty()) {
                return entry.getValue().getFirst();
            }
        }
        return "<absent>";
    }

    /**
     * Bounds untrusted metadata and removes characters that could forge multiline logs.
     */
    static String safe(final String value) {
        final String bounded = value.substring(0, Math.min(value.length(), 240));
        return bounded.replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]", " ")
            + (value.length() > 240 ? "..." : "");
    }
}
