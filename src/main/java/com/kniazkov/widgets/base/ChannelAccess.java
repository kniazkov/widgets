/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.util.UUID;

/**
 * Credentials already separated from application form data by the HTTP boundary.
 */
final class ChannelAccess {
    /**
     * Authenticated transport owner.
     */
    final BrowserSession session;
    /**
     * Per-page secret presented in a header, never a query parameter.
     */
    private final String token;
    /**
     * Server-state epoch presented in a header.
     */
    private final String epoch;

    /**
     * @param session authenticated transport session
     * @param token page channel secret
     * @param epoch server-state epoch
     */
    ChannelAccess(final BrowserSession session, final String token, final String epoch) {
        this.session = session;
        this.token = token;
        this.epoch = epoch;
    }

    /**
     * Checks before accessing models, ACKs, queues, lifetimes or controllers.
     * Null access is allowed only for unbound in-process test/embedding clients;
     * the HTTP boundary always creates session-bound clients and supplies access.
     * @param access HTTP credentials or null for an unbound in-process client
     * @param client target client
     * @param serverId current server epoch
     */
    static void require(final ChannelAccess access, final Client client, final UUID serverId) {
        final BrowserSession owner = client.getTabContext().getBrowserSession();
        if (access == null && owner == null) {
            return;
        }
        if (access == null || owner == null || access.session != owner
                || !serverId.toString().equals(access.epoch)
                || !ProtocolSecurity.matches(client.channelToken(), access.token)) {
            throw new ProtocolAccessException(false);
        }
        if (!owner.isActive()) {
            throw new ProtocolAccessException(true);
        }
    }
}
