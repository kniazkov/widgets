// Linux-only diagnostic probe; not a capacity guarantee or a replacement for browser tests.
// Build both checkouts first; run: node scripts/measure-polling-http.mjs [source checkout].
import process from "node:process";
import { Buffer } from "node:buffer";
import { fileURLToPath } from "node:url";
import { createServer } from "node:net";
import { execFileSync } from "node:child_process";
import { spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import { join, resolve, dirname, delimiter } from "node:path";
import { performance } from "node:perf_hooks";
import { setTimeout as delay } from "node:timers/promises";
import assert from "node:assert/strict";
const project = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const args = process.argv.slice(2);
const browserMode = args.includes("--browser");
const source = resolve(args.find(arg => !arg.startsWith("--")) || project);
const ticks = Number(execFileSync("getconf", ["CLK_TCK"], { encoding: "utf8" }));
const port = await new Promise((accept, reject) => {
    const probe = createServer();
    probe.on("error", reject);
    probe.listen(0, "127.0.0.1", () => {
        const selected = probe.address().port;
        probe.close(error => (error ? reject(error) : accept(selected)));
    });
});
const dependencies = (await readFile(join(source, "target/runtime-classpath.txt"), "utf8")).trim();
const java = spawn(
    process.env.JAVA_HOME ? join(process.env.JAVA_HOME, "bin/java") : "java",
    [
        "-Xmx1g",
        "-cp",
        [join(source, "target/test-classes"), join(source, "target/classes"), dependencies].join(
            delimiter
        ),
        "com.kniazkov.widgets.e2e.E2ETestServer",
        String(port)
    ],
    { stdio: "ignore" }
);
java.on("error", error => {
    console.error(error.message);
});
const origin = `http://127.0.0.1:${port}`;
let browser;
let page;
let headers;
let measured;
async function send(fields, channel) {
    const boundary = "widgets-polling-benchmark";
    const body =
        Object.entries(fields)
            .map(
                ([k, v]) =>
                    `--${boundary}\r\nContent-Disposition: form-data; name="${k}"\r\n\r\n${typeof v === "object" ? JSON.stringify(v) : v}\r\n`
            )
            .join("") + `--${boundary}--\r\n`;
    const extra = channel
        ? {
              "X-Widgets-Channel": channel.channelToken,
              "X-Widgets-Epoch": channel.serverId,
              "X-Widgets-Update-Epoch": channel.updateEpoch
          }
        : {};
    const started = performance.now();
    const response = await fetch(origin, {
        method: "POST",
        headers: {
            ...headers,
            ...extra,
            "Content-Type": `multipart/form-data; boundary=${boundary}`
        },
        body,
        signal: AbortSignal.timeout(15000)
    });
    const text = await response.text();
    assert.equal(response.status, 200);
    if (measured) {
        measured.requests++;
        measured.requestBytes += Buffer.byteLength(body);
        measured.responseBytes += Buffer.byteLength(text);
        measured.latency.push(performance.now() - started);
    }
    return JSON.parse(text);
}
async function each(items, fn) {
    let cursor = 0;
    await Promise.all(
        Array.from({ length: Math.min(16, items.length) }, async () => {
            while (cursor < items.length) {
                const i = cursor++;
                await fn(items[i], i);
            }
        })
    );
}
async function cpu() {
    const text = await readFile(`/proc/${java.pid}/stat`, "utf8");
    const fields = text.slice(text.lastIndexOf(")") + 2).split(" ");
    return ((Number(fields[11]) + Number(fields[12])) * 1000) / ticks;
}
try {
    let bootstrap;
    for (let i = 0; i < 100; i++) {
        try {
            bootstrap = await fetch(origin + "/catalog");
            if (bootstrap.ok) break;
        } catch {}
        await delay(100);
    }
    assert(bootstrap?.ok, "server did not start");
    const html = await bootstrap.text();
    const match = html.match(/configureProtocolSecurity\("([^"]+)", "([^"]+)"\)/);
    assert(match, "missing CSRF bootstrap");
    headers = {
        Origin: origin,
        Cookie: bootstrap.headers
            .getSetCookie()
            .map(c => c.split(";")[0])
            .join("; "),
        "X-Widgets-CSRF": match[1],
        "X-Widgets-Session": match[2]
    };
    if (browserMode) {
        const { chromium } = await import("@playwright/test");
        browser = await chromium.launch();
        page = await browser.newPage();
    }
    for (const count of [1, 100, 1000]) {
        const clients = Array.from({ length: count }, () => null);
        try {
            await each(clients, async (_, i) => {
                const c = await send({
                    action: "new instance",
                    address: "/catalog",
                    browserId: "12345678-1234-4234-8234-123456789abc",
                    mobile: false,
                    clientBuild: bootstrap.headers.get("X-Widgets-Build"),
                    updateVersion: "2",
                    updateCapability: "sequenced-updates"
                });
                assert(c.id);
                clients[i] = c;
                let reply;
                do {
                    reply = await send(
                        {
                            action: "synchronize",
                            client: c.id,
                            events: [],
                            lastUpdate: "#" + (c.lastUpdate || 0)
                        },
                        c
                    );
                    assert.equal(reply.result, true);
                    c.lastUpdate = reply.updateThrough;
                } while (reply.moreUpdates);
                await send(
                    {
                        action: "synchronize",
                        client: c.id,
                        events: [],
                        lastUpdate: "#" + c.lastUpdate
                    },
                    c
                );
            });
            const sync = async c => {
                const r = await send(
                    {
                        action: "synchronize",
                        client: c.id,
                        events: [],
                        lastUpdate: "#" + c.lastUpdate
                    },
                    c
                );
                assert.equal(r.result, true);
                assert.equal(r.updates.length, 0);
            };
            for (let n = 0; n < 3; n++) await each(clients, sync);
            measured = {
                clients: count,
                requests: 0,
                requestBytes: 0,
                responseBytes: 0,
                latency: []
            };
            const before = await cpu(),
                started = performance.now();
            for (let n = 0; n < 10; n++) await each(clients, sync);
            measured.cpuMs = (await cpu()) - before;
            measured.wallMs = performance.now() - started;
            measured.latency.sort((a, b) => a - b);
            measured.p50Ms = measured.latency[Math.floor(measured.latency.length * 0.5)];
            measured.p95Ms = measured.latency[Math.floor(measured.latency.length * 0.95)];
            delete measured.latency;
            const result = measured;
            measured = null;
            if (page) {
                await page.goto(origin + "/local-actions");
                const editor = page.getByRole("textbox", { name: "IME editor" });
                await editor.waitFor();
                let running = true;
                let loadError;
                const load = (async () => {
                    try {
                        while (running) await each(clients, sync);
                    } catch (error) {
                        loadError = error;
                    }
                })();
                result.eventToDomMs = [];
                try {
                    for (let index = 0; index < 10; index++) {
                        const text = "probe" + count + "sample" + index;
                        await editor.fill(text);
                        const start = performance.now();
                        await editor.press("Enter");
                        await page
                            .getByText("Submitted: " + text.toUpperCase(), { exact: true })
                            .waitFor();
                        result.eventToDomMs.push(performance.now() - start);
                    }
                } finally {
                    running = false;
                    await load;
                }
                if (loadError) throw loadError;
            }
            console.log(JSON.stringify(result));
        } finally {
            measured = null;
            await each(clients.filter(Boolean), c => send({ action: "kill", client: c.id }, c));
        }
    }
} finally {
    try {
        await browser?.close();
    } finally {
        java.kill();
    }
}
