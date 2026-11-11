/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import com.kniazkov.webserver.Request;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Strict HTTP-boundary parsing and credentials shared by all protocol actions.
 */
final class ProtocolSecurity {
    /**
     * Cryptographic entropy source, independent of public widget/client IDs.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * No instances.
     */
    private ProtocolSecurity() {
    }

    /**
     * @return 256-bit URL-safe random secret
     */
    static String secret() {
        final byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * @param expected server-side secret
     * @param provided untrusted candidate
     * @return constant-time equality for correctly sized credentials
     */
    static boolean matches(final String expected, final String provided) {
        return provided != null && provided.length() == expected.length()
            && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
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
     * @param value HTTP(S) origin without credentials, query, path or fragment
     * @return normalized origin, or null for invalid input
     */
    static String origin(final String value) {
        if (value == null) {
            return null;
        }
        try {
            final URI uri = URI.create(value);
            final String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !(uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath()))
                    || uri.getPort() > 65535) {
                return null;
            }
            final int port = uri.getPort();
            final boolean defaultPort = port == -1
                || (port == 80 && "http".equalsIgnoreCase(scheme))
                || (port == 443 && "https".equalsIgnoreCase(scheme));
            return scheme.toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                + (defaultPort ? "" : ":" + port);
        } catch (final IllegalArgumentException invalid) {
            return null;
        }
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
                : origin((options.getSslOptions().isPresent() ? "https://" : "http://") + host))
            : options.getPublicOrigin();
        final String rawOrigin = header(request, "Origin");
        final boolean hasOrigin = request.getHeaders().getValues().keySet().stream()
            .anyMatch(name -> "Origin".equalsIgnoreCase(name));
        final String site = header(request, "Sec-Fetch-Site");
        final boolean hasSite = request.getHeaders().getValues().keySet().stream()
            .anyMatch(name -> "Sec-Fetch-Site".equalsIgnoreCase(name));
        return expected != null && (!hasOrigin || expected.equals(origin(rawOrigin)))
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
