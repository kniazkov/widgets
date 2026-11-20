import assert from "node:assert/strict";
import { Buffer } from "node:buffer";
import { performance } from "node:perf_hooks";

export const pointer = {
    type: "mouse",
    primary: true,
    buttons: 0,
    pressure: 0,
    keys: { ctrl: false, alt: false, shift: false, meta: false },
    position: Object.fromEntries(
        ["element", "client", "page", "screen"].map(k => [k, { x: 10, y: 20 }])
    )
};

export async function protocol(origin) {
    const bootstrap = await fetch(origin + "/load", { signal: AbortSignal.timeout(15000) });
    assert.equal(bootstrap.status, 200);
    const html = await bootstrap.text();
    const match = html.match(/configureProtocolSecurity\("([^"]+)", "([^"]+)"\)/);
    assert(match, "Missing transport bootstrap");
    const headers = {
        Origin: origin,
        Cookie: bootstrap.headers
            .getSetCookie()
            .map(c => c.split(";")[0])
            .join("; "),
        "X-Widgets-CSRF": match[1],
        "X-Widgets-Session": match[2]
    };
    const live = new Set();
    let meter;
    async function send(fields, channel, binary) {
        const boundary = "widgets-load-fixture";
        const parts = Object.entries(fields).map(([k, v]) =>
            Buffer.from(
                `--${boundary}\r\nContent-Disposition: form-data; name="${k}"\r\n\r\n${typeof v === "object" ? JSON.stringify(v) : v}\r\n`
            )
        );
        if (binary)
            parts.push(
                Buffer.from(
                    `--${boundary}\r\nContent-Disposition: form-data; name="chunk"; filename="fixture.bin"\r\nContent-Type: application/octet-stream\r\n\r\n`
                ),
                binary,
                Buffer.from("\r\n")
            );
        parts.push(Buffer.from(`--${boundary}--\r\n`));
        const body = Buffer.concat(parts);
        const started = performance.now();
        const response = await fetch(origin, {
            method: "POST",
            headers: {
                ...headers,
                "Content-Type": `multipart/form-data; boundary=${boundary}`,
                ...(channel
                    ? {
                          "X-Widgets-Channel": channel.channelToken,
                          "X-Widgets-Epoch": channel.serverId,
                          "X-Widgets-Update-Epoch": channel.updateEpoch
                      }
                    : {})
            },
            body,
            signal: AbortSignal.timeout(15000)
        });
        const text = await response.text();
        if (meter) {
            meter.requests++;
            meter.requestBytes += body.length;
            meter.responseBytes += Buffer.byteLength(text);
            meter.httpMs.push(performance.now() - started);
        }
        assert.equal(response.status, 200, "HTTP protocol request failed");
        return JSON.parse(text);
    }
    function apply(c, reply) {
        for (const update of reply.updates || []) {
            if (update.action === "create widget" && update.type === "button" && !c.button)
                c.button = update.widget;
            if (update.action === "create widget" && update.type === "file loader")
                c.loader = update.widget;
            if (update.action === "set text" && /^Count: \d+$/.test(update.text))
                c.count = Number(update.text.slice(7));
            if (update.action === "set text" && /^Uploads: \d+$/.test(update.text))
                c.uploads = Number(update.text.slice(9));
        }
        if (reply.updateThrough !== undefined) c.lastUpdate = reply.updateThrough;
    }
    async function sync(c, events = [], checkpoint = c.lastUpdate) {
        const reply = await send(
            { action: "synchronize", client: c.id, events, lastUpdate: "#" + checkpoint },
            c
        );
        apply(c, reply);
        return reply;
    }
    async function drain(c) {
        let reply;
        do {
            reply = await sync(c);
            assert.equal(reply.result, true);
            assert(!reply.updateResyncRequired, "Snapshot unavailable");
        } while (reply.moreUpdates);
        return reply;
    }
    async function create(address = "/load", tabId) {
        const c = await send({
            action: "new instance",
            address,
            browserId: "12345678-1234-4234-8234-123456789abc",
            mobile: false,
            clientBuild: bootstrap.headers.get("X-Widgets-Build"),
            updateVersion: "2",
            updateCapability: "sequenced-updates",
            ...(tabId ? { tabId } : {})
        });
        assert(c.id, "Client creation failed");
        c.lastUpdate = 0;
        c.event = 0;
        live.add(c);
        await drain(c);
        await sync(c);
        return c;
    }
    async function kill(c) {
        await send({ action: "kill", client: c.id }, c);
        live.delete(c);
    }
    function clicks(c, count) {
        return Array.from({ length: count }, () => ({
            id: "#" + ++c.event,
            widget: c.button,
            type: "click",
            data: pointer
        }));
    }
    return {
        send,
        sync,
        drain,
        create,
        kill,
        clicks,
        apply,
        live,
        measure(value) {
            meter = value;
        }
    };
}
