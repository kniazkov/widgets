/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Physical transport only: it never assigns event IDs, advances ACKs or retries commands.
// driver.connect(deliver, disconnected), driver.send(packet, done)->cancel, driver.close().
function createProtocolTransport(driver, { maxInFlight = 64, maxChannels = 64 } = {}) {
    if (
        !Number.isInteger(maxInFlight) ||
        maxInFlight < 1 ||
        !Number.isInteger(maxChannels) ||
        maxChannels < 1
    )
        throw new Error("Invalid transport limits");
    let state = "new";
    const pending = new Set(),
        channels = new Set(),
        receivers = new Set();
    const deliver = message => {
        if (state === "open") for (const receive of receivers) receive(message);
    };
    const transport = {
        get state() {
            return state;
        },
        get pending() {
            return pending.size;
        },
        connect() {
            if (state !== "new") throw new Error("Transport already connected or closed");
            state = "connecting";
            const ready = () => {
                if (state === "connecting") state = "open";
            };
            try {
                const result = driver.connect?.(deliver, () => transport.close());
                if (result?.then)
                    return result.then(ready, error => {
                        transport.close();
                        throw error;
                    });
                ready();
            } catch (error) {
                transport.close();
                throw error;
            }
        },
        receive(listener) {
            if (state === "closed") throw new Error("Transport closed");
            receivers.add(listener);
            return () => receivers.delete(listener);
        },
        send(packet, callback) {
            if (state !== "open" || pending.size >= maxInFlight) {
                callback?.(null); // uncertain/not sent; protocol owner decides whether to retry
                return () => {};
            }
            const operation = { cancel: null, settled: false, abort: null };
            const complete = data => {
                if (operation.settled) return;
                operation.settled = true;
                pending.delete(operation);
                callback?.(data);
            };
            operation.cancel = () => {
                try {
                    operation.abort?.();
                } finally {
                    complete(null);
                }
            };
            pending.add(operation);
            try {
                operation.abort = driver.send(packet, complete);
            } catch {
                complete(null);
            }
            return operation.cancel;
        },
        channel(credentials) {
            if (state !== "open" || channels.size >= maxChannels)
                throw new Error("Channel limit or closed transport");
            let closed = false;
            const operations = new Set();
            const channel = {
                send(query, callback, files) {
                    if (closed) {
                        callback?.(null);
                        return () => {};
                    }
                    const entry = { cancel: null, done: false };
                    operations.add(entry);
                    entry.cancel = transport.send({ query, channel: credentials, files }, data => {
                        entry.done = true;
                        operations.delete(entry);
                        callback?.(data);
                    });
                    return entry.cancel;
                },
                close() {
                    if (closed) return;
                    closed = true;
                    channels.delete(channel);
                    for (const entry of [...operations]) entry.cancel?.();
                    operations.clear();
                }
            };
            channels.add(channel);
            return channel;
        },
        close() {
            if (state === "closed") return;
            state = "closed";
            let failure;
            for (const operation of [...pending]) {
                try {
                    operation.cancel();
                } catch (error) {
                    failure ||= error;
                }
            }
            for (const channel of [...channels]) channel.close();
            receivers.clear();
            driver.close?.();
            if (failure) throw failure;
        }
    };
    return transport;
}
