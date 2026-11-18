/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * Transport-independent credentials and origin normalization.
 */
final class SecurityPrimitives {
    /**
     * Cryptographic entropy source, independent of public widget/client IDs.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * No instances.
     */
    private SecurityPrimitives() {
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

}
