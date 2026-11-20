/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.Request;
import java.util.List;
import java.util.Map;

/**
 * Strict HTTP-boundary parsing and credentials shared by all protocol actions.
 */
final class ProtocolSecurity {
    /**
     * No instances.
     */
    private ProtocolSecurity() {
    }

    /**
     * @param request HTTP request
     * @param name header name
     * @return single value, or null if absent/ambiguous
     */
    static String header(final Request request, final String name) {
        String result = null;
        for (final Map.Entry<String, List<String>> item
                : request.getHeaders().getValues().entrySet()) {
            if (name.equalsIgnoreCase(item.getKey())) {
                if (result != null || item.getValue().size() != 1) {
                    return null;
                }
                result = item.getValue().getFirst();
            }
        }
        return result;
    }

    /**
     * @param options server options
     * @return whether transport credentials require HTTPS
     */
    static boolean secureCookie(final Options options) {
        return options.getPublicOrigin().startsWith("https://")
            || (options.getPublicOrigin().isEmpty() && options.getSslOptions().isPresent());
    }

    /**
     * @param options server options
     * @return host-only cookie name (browser-enforced prefix on HTTPS)
     */
    static String cookieName(final Options options) {
        return secureCookie(options) ? "__Host-widgets-session" : "widgets-session";
    }

    /**
     * @param request HTTP request
     * @param options configured public origin, otherwise direct listener origin
     * @return whether browser origin/fetch metadata allows this protocol request
     */
    static boolean sameOrigin(final Request request, final Options options) {
        final String host = header(request, "Host");
        final String expected = options.getPublicOrigin().isEmpty()
            ? (host == null ? null
                : SecurityPrimitives.origin(
                    (options.getSslOptions().isPresent() ? "https://" : "http://") + host))
            : options.getPublicOrigin();
        final String rawOrigin = header(request, "Origin");
        final boolean hasOrigin = request.getHeaders().getValues().keySet().stream()
            .anyMatch(name -> "Origin".equalsIgnoreCase(name));
        final String site = header(request, "Sec-Fetch-Site");
        final boolean hasSite = request.getHeaders().getValues().keySet().stream()
            .anyMatch(name -> "Sec-Fetch-Site".equalsIgnoreCase(name));
        return expected != null
            && (!hasOrigin || expected.equals(SecurityPrimitives.origin(rawOrigin)))
            && (!hasSite || "same-origin".equals(site));
    }

    /**
     * @param request HTTP request
     * @param options server options
     * @return unambiguous cookie credential, or null
     */
    static String cookie(final Request request, final Options options) {
        final String name = cookieName(options);
        int count = 0;
        for (final Map.Entry<String, List<String>> item
                : request.getHeaders().getValues().entrySet()) {
            if ("Cookie".equalsIgnoreCase(item.getKey())) {
                for (final String line : item.getValue()) {
                    for (final String part : line.split(";")) {
                        if (part.strip().startsWith(name + "=")) {
                            count++;
                        }
                    }
                }
            }
        }
        return count == 1 ? request.getCookies().get(name) : null;
    }
}
