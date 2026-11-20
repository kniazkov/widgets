/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// One physical socket per document, independently authenticated page channels and checkpoints.
// Only synchronize is replay-safe here. Creation, cleanup, diagnostics and binary upload stay HTTP.
function createPushCoordinator({
    url,
    auth,
    http,
    socketFactory,
    maxBytes = 65536,
    leaseMillis = 5000
}) {
    const channels = new Map(),
        pending = new Map();
    let socket = null,
        ready = false,
        stopped = false,
        reconnect = null,
        watchdog = null;
    let failures = 0,
        sequence = 0,
        lastLease = 0,
        lastSeen = 0;
    socketFactory ||= url => new WebSocket(url, "widgets.stream.v1");
    function write(packet) {
        if (!socket || socket.readyState !== 1) return false;
        const text = JSON.stringify(packet);
        if (new TextEncoder().encode(text).length > maxBytes || socket.bufferedAmount > 262144)
            return false;
        try {
            socket.send(text);
            return true;
        } catch {
            return false;
        }
    }
    function finish(entry, data) {
        if (!pending.delete(entry.id)) return;
        clearTimeout(entry.timer);
        entry.done(data);
    }
    function disconnect(current) {
        if (socket !== current) return;
        socket = null;
        ready = false;
        clearInterval(watchdog);
        watchdog = null;
        current?.close();
        for (const channel of channels.values()) channel.active = false;
        for (const entry of [...pending.values()]) finish(entry, null);
        for (const channel of channels.values()) channel.wake();
        if (!stopped && channels.size && reconnect === null) {
            reconnect = setTimeout(() => {
                reconnect = null;
                connect();
            }, transportRetryDelay(++failures));
        }
    }
    function connect() {
        if (socket || stopped || !channels.size || reconnect !== null) return;
        let current;
        try {
            current = socketFactory(url);
        } catch {
            reconnect = setTimeout(() => {
                reconnect = null;
                connect();
            }, transportRetryDelay(++failures));
            return;
        }
        socket = current;
        sequence = 0;
        const started = Date.now();
        lastSeen = started;
        lastLease = started;
        current.onopen = () => {
            if (socket === current && !write({ type: "auth", version: 1, ...auth() }))
                disconnect(current);
        };
        current.onclose = current.onerror = () => disconnect(current);
        current.onmessage = event => {
            if (socket !== current) return;
            try {
                const packet = JSON.parse(event.data);
                if (!ready) {
                    if (packet.type !== "ready" || packet.version !== 1)
                        throw new Error("Invalid ready");
                    ready = true;
                    failures = 0;
                    for (const [client, channel] of channels) {
                        if (!write({ type: "subscribe", client, channel: channel.credentials() }))
                            throw new Error("Subscribe overflow");
                    }
                } else if (packet.type === "subscribed") {
                    const channel = channels.get(packet.client);
                    if (channel) {
                        channel.active = true;
                        channel.wake();
                    }
                } else if (packet.type === "dirty") {
                    channels.get(packet.client)?.wake();
                } else if (packet.type === "lease") {
                    lastSeen = Date.now();
                } else if (packet.type === "response" || packet.type === "http") {
                    const entry = pending.get(packet.id);
                    if (!entry) return; // old/cancelled operation; never apply to another page
                    if (packet.type === "http") {
                        pending.delete(entry.id);
                        clearTimeout(entry.timer);
                        entry.abort = http(entry.query, entry.done, entry.credentials);
                    } else finish(entry, JSON.stringify(packet.data));
                } else throw new Error("Invalid stream packet");
            } catch {
                disconnect(current);
            }
        };
        watchdog = setInterval(
            () => {
                if (socket !== current) return;
                const now = Date.now();
                if (
                    (!ready && now - started > 8000) ||
                    (ready && now - lastSeen > Math.max(10000, leaseMillis * 3))
                ) {
                    disconnect(current);
                } else if (ready && now - lastLease >= leaseMillis) {
                    lastLease = now;
                    if (!write({ type: "lease" })) disconnect(current);
                }
            },
            Math.min(250, leaseMillis)
        );
    }
    return {
        register(client, credentials, wake) {
            if (stopped || channels.size >= 64 || channels.has(client)) return null;
            const channel = { credentials, wake, active: false };
            channels.set(client, channel);
            if (ready) {
                if (!write({ type: "subscribe", client, channel: credentials() }))
                    disconnect(socket);
            } else connect();
            return {
                available: () => ready && channel.active,
                send(query, done) {
                    if (!ready || !channel.active || pending.size >= 64)
                        return http(query, done, credentials());
                    if (!write({ type: "ackHint", client })) {
                        disconnect(socket);
                        return http(query, done, credentials());
                    }
                    const id = ++sequence;
                    if (!Number.isSafeInteger(id)) {
                        disconnect(socket);
                        return http(query, done, credentials());
                    }
                    const entry = {
                        id,
                        query,
                        done,
                        credentials: credentials(),
                        timer: null,
                        abort: null
                    };
                    pending.set(id, entry);
                    entry.timer = setTimeout(() => disconnect(socket), 10000);
                    if (!write({ type: "request", id, query, channel: entry.credentials })) {
                        pending.delete(id);
                        clearTimeout(entry.timer);
                        return http(query, done, entry.credentials);
                    }
                    return () => {
                        if (entry.abort) entry.abort();
                        else finish(entry, null);
                    };
                },
                dispose() {
                    if (!channels.delete(client)) return;
                    if (ready && !write({ type: "unsubscribe", client })) disconnect(socket);
                    for (const entry of [...pending.values()])
                        if (entry.query.client === client) finish(entry, null);
                    if (!channels.size) {
                        clearTimeout(reconnect);
                        reconnect = null;
                        disconnect(socket);
                    }
                }
            };
        },
        close() {
            stopped = true;
            clearTimeout(reconnect);
            reconnect = null;
            channels.clear();
            disconnect(socket);
        },
        get state() {
            return ready ? "open" : stopped ? "closed" : "connecting";
        },
        get pending() {
            return pending.size;
        },
        get subscriptions() {
            return channels.size;
        }
    };
}

let documentPushCoordinator = null;
function registerPushChannel(configuration, client, credentials, wake) {
    if (configuration.pushPath !== "/widgets-stream" || typeof WebSocket !== "function")
        return null;
    if (!documentPushCoordinator) {
        documentPushCoordinator = createPushCoordinator({
            url: server.replace(/^http/, "ws") + configuration.pushPath,
            auth: () => ({ session: protocolSession, csrf: protocolCsrf }),
            http: (query, done, channel) => sendRequest(query, done, "post", null, channel),
            maxBytes: configuration.pushMessageBytes,
            leaseMillis: Math.max(25, Math.min(5000, configuration.pollingMaxDelay / 2))
        });
        window.addEventListener("pagehide", event => {
            if (!event.persisted) documentPushCoordinator.close();
        });
    }
    return documentPushCoordinator.register(client, credentials, wake);
}
