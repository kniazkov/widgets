/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.net.URI;

/**
 * Explicit limits for the optional loopback WebSocket listener behind a TLS proxy.
 *
 * @param port loopback listener port, zero selects an ephemeral port
 * @param publicOrigin exact externally visible origin shared with HTTP
 * @param connections maximum reserved, unauthenticated and authenticated sockets combined
 * @param workers maximum concurrent application callbacks, separate from HTTP workers
 * @param tasks maximum queued application callbacks
 * @param messageBytes maximum UTF-8 text message size
 * @param queuedMessages maximum queued outgoing messages including the current write
 * @param queuedBytes maximum outgoing UTF-8 bytes including the current write
 * @param authMillis deadline for the first authentication message
 * @param heartbeatMillis interval between server pings
 * @param timeoutMillis pong/write/handler deadline; must exceed heartbeat interval
 */
public record StreamOptions(int port, String publicOrigin, int connections, int workers,
        int tasks, int messageBytes, int queuedMessages, int queuedBytes,
        long authMillis, long heartbeatMillis, long timeoutMillis) {
    /**
     * Validates fixed resource budgets and requires TLS except on local test origins.
     */
    public StreamOptions {
        final String normalized = ProtocolSecurity.origin(publicOrigin);
        if (normalized == null || port < 0 || port > 65535 || connections < 1 || workers < 1
                || tasks < 1 || messageBytes < 256 || queuedMessages < 1
                || queuedBytes < messageBytes || authMillis < 100 || heartbeatMillis < 100
                || timeoutMillis <= heartbeatMillis) {
            throw new IllegalArgumentException("Invalid stream options");
        }
        final URI uri = URI.create(normalized);
        if (!"https".equals(uri.getScheme()) && !"127.0.0.1".equals(uri.getHost())
                && !"localhost".equals(uri.getHost()) && !"[::1]".equals(uri.getHost())) {
            throw new IllegalArgumentException("Public stream origin must use HTTPS");
        }
        publicOrigin = normalized;
    }

    /**
     * @param port loopback listener port
     * @param origin externally visible HTTP/WS origin
     * @return conservative bounded defaults, to be sized for the deployment
     */
    public static StreamOptions defaults(final int port, final String origin) {
        return new StreamOptions(port, origin, 1024, 4, 64, 65536, 64, 262144,
            5000, 20000, 40000);
    }
}
