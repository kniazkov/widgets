import { spawn, execFileSync } from "node:child_process";
import { readFile, mkdir, writeFile, rm } from "node:fs/promises";
import { createServer as tlsServer } from "node:https";
import { request } from "node:http";
import { createServer } from "node:net";
import { join, delimiter } from "node:path";
import { setTimeout as delay } from "node:timers/promises";
import process from "node:process";
import { performance } from "node:perf_hooks";
import assert from "node:assert/strict";
import { chromium } from "@playwright/test";
import { Transform } from "node:stream";

// A TLS reverse proxy only: WebSocket frames travel directly to the Java adapter.
const output = "target/push-probe";
await mkdir(output, { recursive: true });
await rm(`${output}/metrics.json`, { force: true });
const proxyDiagnostics = [];
let httpRequests = 0,
    lag = 0;
execFileSync(
    "openssl",
    [
        "req",
        "-x509",
        "-newkey",
        "rsa:2048",
        "-nodes",
        "-keyout",
        `${output}/key.pem`,
        "-out",
        `${output}/cert.pem`,
        "-days",
        "1",
        "-subj",
        "/CN=localhost"
    ],
    { stdio: "ignore" }
);
async function freePort() {
    const server = createServer();
    await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
    const port = server.address().port;
    await new Promise(resolve => server.close(resolve));
    return port;
}
const httpPort = await freePort();
const streamPort = await freePort();
const proxy = tlsServer(
    { key: await readFile(`${output}/key.pem`), cert: await readFile(`${output}/cert.pem`) },
    (req, res) => {
        if (req.method === "POST") httpRequests++;
        if (req.url === "/stream-witness") {
            res.writeHead(200, { "Content-Type": "text/html" });
            res.end("<!doctype html><title>Stream witness</title>");
            return;
        }
        const upstream = request(
            {
                hostname: "127.0.0.1",
                port: httpPort,
                path: req.url,
                method: req.method,
                headers: req.headers
            },
            reply => {
                res.writeHead(reply.statusCode, reply.headers);
                reply.pipe(res);
            }
        );
        upstream.on("error", () => {
            res.writeHead(502);
            res.end();
        });
        req.pipe(upstream);
    }
);
const tunnels = new Set();
let blocked = false;
proxy.on("upgrade", (req, socket, head) => {
    if (blocked || req.url !== "/widgets-stream") {
        socket.end("HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\n\r\n");
        return;
    }
    const upstream = request({
        hostname: "127.0.0.1",
        port: streamPort,
        path: req.url,
        headers: req.headers
    });
    upstream.on("upgrade", (res, remote, remainder) => {
        socket.write(
            `HTTP/1.1 101 Switching Protocols\r\n${Object.entries(res.headers)
                .map(([key, value]) => `${key}: ${value}\r\n`)
                .join("")}\r\n`
        );
        if (head.length) remote.write(head);
        if (remainder.length) socket.write(remainder);
        tunnels.add(socket);
        socket.on("close", () => {
            tunnels.delete(socket);
            remote.destroy();
        });
        remote.on("close", () => socket.destroy());
        remote.on("error", () => socket.destroy());
        socket.on("error", () => remote.destroy());
        const throttle = () =>
            new Transform({
                transform(chunk, encoding, done) {
                    if (lag) setTimeout(() => done(null, chunk), lag);
                    else done(null, chunk);
                }
            });
        socket.pipe(throttle()).pipe(remote).pipe(throttle()).pipe(socket);
    });
    upstream.on("response", res => {
        proxyDiagnostics.push({ upgradeStatus: res.statusCode });
        socket.end(`HTTP/1.1 ${res.statusCode} Rejected\r\nContent-Length: 0\r\n\r\n`);
        res.resume();
    });
    upstream.on("error", error => {
        proxyDiagnostics.push({ upstreamError: error.code });
        socket.destroy();
    });
    upstream.end();
});
await new Promise(resolve => proxy.listen(0, "127.0.0.1", resolve));
const origin = `https://127.0.0.1:${proxy.address().port}`;
const classpath = [
    "target/test-classes",
    "target/classes",
    (await readFile("target/runtime-classpath.txt", "utf8")).trim()
].join(delimiter);
const java = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, "bin/java") : "java";
let child,
    browser,
    errors = "";
async function stop() {
    if (!child || child.exitCode !== null) return;
    const exited = new Promise(resolve => child.once("exit", resolve));
    child.kill();
    await exited;
}
async function gauges() {
    for (let attempt = 0; attempt < 40; attempt++) {
        try {
            return JSON.parse(await readFile(`${output}/metrics.json`, "utf8"));
        } catch {
            await delay(25);
        }
    }
    throw new Error("Missing fixture journal");
}
async function start(mode) {
    await stop();
    await rm(`${output}/metrics.json`, { force: true });
    child = spawn(
        java,
        [
            "-Xmx1g",
            "-cp",
            classpath,
            "com.kniazkov.widgets.e2e.PushTestServer",
            String(httpPort),
            String(streamPort),
            origin,
            `${output}/metrics.json`,
            mode
        ],
        { stdio: ["ignore", "ignore", "pipe"] }
    );
    child.stderr.on("data", data => {
        errors = (errors + data).slice(-64000);
    });
    for (let attempt = 0; attempt < 150; attempt++) {
        if (child.exitCode !== null) throw new Error("Fixture exited during startup");
        try {
            JSON.parse(await readFile(`${output}/metrics.json`, "utf8"));
            return;
        } catch {
            await delay(100);
        }
    }
    throw new Error("Fixture startup timeout");
}
async function waitForGauge(predicate, timeout = 15000) {
    const deadline = Date.now() + timeout;
    while (Date.now() < deadline) {
        const sample = await gauges();
        if (predicate(sample)) return sample;
        await delay(100);
    }
    throw new Error(`Resource condition failed: ${JSON.stringify(await gauges())}`);
}
const report = {
    commit: execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim(),
    tls: true,
    nativeJavaWebSocket: true,
    documents: 8,
    clicksPerDocument: 20,
    scope: "Integration load sample, not a capacity or statistically controlled heap benchmark",
    runs: [],
    checks: [],
    proxyDiagnostics
};
try {
    browser = await chromium.launch();
    report.chromium = browser.version();
    for (const mode of ["http", "push"]) {
        await start(mode);
        const context = await browser.newContext({ ignoreHTTPSErrors: true });
        await context.addInitScript(() => {
            const NativeSocket = WebSocket;
            window.WebSocket = class extends NativeSocket {
                constructor(...args) {
                    super(...args);
                    this.addEventListener("message", event => {
                        if (!window.dropPushCount) return;
                        try {
                            const packet = JSON.parse(event.data);
                            if (
                                packet.type === "response" &&
                                JSON.stringify(packet.data).includes(
                                    `Count: ${window.dropPushCount}`
                                )
                            ) {
                                window.dropPushCount = null;
                                event.stopImmediatePropagation();
                                this.close();
                            }
                        } catch {
                            /* not an application frame */
                        }
                    });
                }
            };
        });
        const pages = [];
        let syncFrames = 0,
            controlFrames = 0;
        for (let i = 0; i < report.documents; i++) {
            const page = await context.newPage();
            page.setDefaultTimeout(30000);
            page.on("websocket", socket =>
                socket.on("framesent", frame => {
                    try {
                        if (JSON.parse(frame.payload).type === "request") syncFrames++;
                        else controlFrames++;
                    } catch {
                        /* no secret frame bodies enter the report */
                    }
                })
            );
            await page.goto(origin);
            await page.getByText("Count: 0", { exact: true }).waitFor();
            await page.evaluate(() => document.fonts.ready);
            if (mode === "push")
                await page.waitForFunction(() => documentPushCoordinator?.state === "open");
            pages.push(page);
        }
        await delay(500);
        const before = await gauges();
        httpRequests = 0;
        syncFrames = 0;
        controlFrames = 0;
        const latencies = [];
        const started = performance.now();
        await Promise.all(
            pages.map(async page => {
                for (let count = 1; count <= report.clicksPerDocument; count++) {
                    const time = performance.now();
                    await page.getByRole("button", { name: "Increment", exact: true }).click();
                    await page.getByText(`Count: ${count}`, { exact: true }).waitFor();
                    latencies.push(performance.now() - time);
                }
            })
        );
        const elapsed = performance.now() - started;
        await delay(500);
        assert.equal((await gauges()).effects, report.documents * report.clicksPerDocument);
        const workRequests = { http: httpRequests, synchronizeFrames: syncFrames, controlFrames };
        httpRequests = 0;
        syncFrames = 0;
        controlFrames = 0;
        await delay(12000);
        const idleRequests = { http: httpRequests, synchronizeFrames: syncFrames, controlFrames };
        if (mode === "push") {
            assert.equal(idleRequests.http, 0);
            assert.equal(idleRequests.synchronizeFrames, 0);
        } else assert.ok(idleRequests.http > 0);
        latencies.sort((a, b) => a - b);
        const after = await gauges();
        report.runs.push({
            mode,
            elapsedMillis: elapsed,
            samples: latencies.length,
            p50Millis: latencies[Math.floor(latencies.length * 0.5)],
            p95Millis: latencies[Math.floor(latencies.length * 0.95)],
            workRequests,
            idleRequests,
            heapBefore: before.heapBytes,
            heapAfter: after.heapBytes,
            connections: after.connections || 0,
            subscriptions: after.subscriptions || 0
        });
        const page = pages[0];
        let count = report.clicksPerDocument;
        if (mode === "push") {
            assert.equal(after.connections, report.documents);
            assert.equal(after.subscriptions, report.documents);
            // Cached pages retain independent channels while sharing the document's socket.
            await page.getByRole("button", { name: "Background increment", exact: true }).click();
            await page.getByRole("button", { name: "Next page", exact: true }).click();
            await page.getByText("Page B", { exact: true }).waitFor();
            await page.getByText(`Count: ${++count}`, { exact: true }).waitFor();
            const constructed = (await waitForGauge(g => g.pages === report.documents + 1)).pages;
            await page.goBack();
            await page.getByText("Page A", { exact: true }).waitFor();
            await page.getByText(`Count: ${count}`, { exact: true }).waitFor();
            await page.goForward();
            await page.getByText("Page B", { exact: true }).waitFor();
            assert.equal((await gauges()).pages, constructed);
            await waitForGauge(g => g.subscriptions === report.documents + 1);
            assert.equal((await gauges()).connections, report.documents);
            await pages[1]
                .getByText(`Count: ${report.clicksPerDocument}`, { exact: true })
                .waitFor();
            report.checks.push(
                "eight documents; cached pages share one socket; Back/Forward preserves state and independent tab counters"
            );
            // Drop a response after the business action committed; retry must keep its event ID.
            const effects = (
                await waitForGauge(
                    g => g.effects === report.documents * report.clicksPerDocument + 1
                )
            ).effects;
            await page.evaluate(expected => {
                window.dropPushCount = expected;
            }, count + 1);
            await page.getByRole("button", { name: "Increment", exact: true }).click();
            await page.getByText(`Count: ${++count}`, { exact: true }).waitFor();
            await waitForGauge(g => g.effects === effects + 1);
            assert.equal((await gauges()).effects, effects + 1);
            report.checks.push("lost stream ACK resumes through HTTP without duplicate action");
            blocked = true;
            for (const socket of tunnels) socket.destroy();
            await page.getByRole("button", { name: "Increment", exact: true }).click();
            await page.getByText(`Count: ${++count}`, { exact: true }).waitFor();
            blocked = false;
            await waitForGauge(g => g.connections === report.documents, 30000);
            lag = 200;
            for (let i = 0; i < 3; i++) {
                await page.getByRole("button", { name: "Increment", exact: true }).click();
                await page.getByText(`Count: ${++count}`, { exact: true }).waitFor();
            }
            lag = 0;
            report.checks.push(
                "proxy outage falls back to HTTP; reconnect and delayed native frame delivery preserve actions"
            );
        }
        const chooser = page.waitForEvent("filechooser");
        await page.getByRole("button", { name: "Upload fixture", exact: true }).click();
        await (
            await chooser
        ).setFiles({
            name: "fixture.bin",
            mimeType: "application/octet-stream",
            buffer: Buffer.alloc(65536, 9)
        });
        await page.getByText("Uploads: 1", { exact: true }).waitFor();
        if (mode === "push") {
            // Process state is ephemeral: restart must bootstrap a new generation, not replay commands.
            await start("push");
            await page.getByText("Count: 0", { exact: true }).waitFor({ timeout: 45000 });
            await page.getByRole("button", { name: "Increment", exact: true }).click();
            await page.getByText("Count: 1", { exact: true }).waitFor();
            await page.getByRole("button", { name: "Revoke session", exact: true }).click();
            await page.getByText("Count: 0", { exact: true }).waitFor({ timeout: 45000 });
            await page.getByRole("button", { name: "Increment", exact: true }).click();
            await page.getByText("Count: 1", { exact: true }).waitFor();
            report.checks.push(
                "binary upload; server restart and logout bootstrap fresh state without replaying old commands"
            );
        }
        await context.close();
        if (mode === "push") {
            const final = await waitForGauge(g =>
                [
                    "connections",
                    "subscriptions",
                    "pushTasks",
                    "pushWorkers",
                    "outgoingBytes",
                    "outgoingMessages"
                ].every(key => g[key] === 0)
            );
            report.finalGauges = final;
        }
    }
    report.passed = true;
} finally {
    await writeFile(`${output}/report.json`, JSON.stringify(report, null, 2));
    await writeFile(`${output}/server.log`, errors);
    await browser?.close();
    for (const socket of tunnels) socket.destroy();
    proxy.closeAllConnections();
    await new Promise(resolve => proxy.close(resolve));
    await stop();
}
console.log(JSON.stringify(report, null, 2));
