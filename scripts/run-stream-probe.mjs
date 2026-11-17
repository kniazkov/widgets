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
import WebSocket from "ws";

// A TLS reverse proxy only: WebSocket frames travel directly to the Java adapter.
const output = "target/stream-probe";
await mkdir(output, { recursive: true });
await rm(`${output}/metrics.json`, { force: true });
const proxyDiagnostics = [];
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
        socket.pipe(remote).pipe(socket);
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
const child = spawn(
    java,
    [
        "-cp",
        classpath,
        "com.kniazkov.widgets.e2e.StreamTestServer",
        String(httpPort),
        String(streamPort),
        origin,
        `${output}/metrics.json`
    ],
    { stdio: ["ignore", "ignore", "pipe"] }
);
let errors = "";
child.stderr.on("data", data => {
    errors = (errors + data).slice(-64000);
});
let browser;
async function gauges() {
    for (let attempt = 0; attempt < 20; attempt++) {
        try {
            return JSON.parse(await readFile(`${output}/metrics.json`, "utf8"));
        } catch {
            await delay(25);
        }
    }
    throw new Error("No valid resource sample");
}
const report = {
    commit: execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim(),
    tls: true,
    nativeJavaWebSocket: true,
    proxyDiagnostics,
    checks: []
};
try {
    for (let i = 0; i < 100; i++) {
        try {
            if (
                (await fetch(`http://127.0.0.1:${httpPort}`, { signal: AbortSignal.timeout(300) }))
                    .ok
            )
                break;
        } catch {
            /* startup */
        }
        if (i === 99) throw new Error("Fixture startup timeout");
        await delay(100);
    }
    // HTTP is started first; its readiness alone says nothing about the stream listener.
    for (let i = 0; i < 100; i++) {
        if (child.exitCode !== null) throw new Error("Fixture exited before stream readiness");
        try {
            const ready = JSON.parse(await readFile(`${output}/metrics.json`, "utf8"));
            if (ready.connections === 0) break;
        } catch {
            /* the journal is created only after the stream listener starts */
        }
        if (i === 99) throw new Error("Stream listener startup timeout");
        await delay(100);
    }
    browser = await chromium.launch();
    const context = await browser.newContext({ ignoreHTTPSErrors: true });
    const page = await context.newPage();
    page.setDefaultTimeout(15000);
    await page.goto(origin);
    await page.getByRole("button", { name: "Increment", exact: true }).waitFor();
    const auth = await page.evaluate(() => ({
        type: "auth",
        version: 1,
        session: protocolSession,
        csrf: protocolCsrf
    }));
    await page.evaluate(() => {
        window.streams = [];
        window.openStream = () =>
            new Promise((resolve, reject) => {
                const ws = new WebSocket(
                    `${location.origin.replace("https:", "wss:")}/widgets-stream`,
                    "widgets.stream.v1"
                );
                const deadline = setTimeout(() => {
                    ws.close();
                    reject(new Error("auth timeout"));
                }, 8000);
                ws.onopen = () =>
                    ws.send(
                        JSON.stringify({
                            type: "auth",
                            version: 1,
                            session: protocolSession,
                            csrf: protocolCsrf
                        })
                    );
                ws.onerror = () => {
                    clearTimeout(deadline);
                    reject(new Error("upgrade failed"));
                };
                ws.onmessage = event => {
                    if (JSON.parse(event.data).type === "ready") {
                        clearTimeout(deadline);
                        window.streams.push(ws);
                        resolve(true);
                    }
                };
            });
    });
    for (let i = 0; i < 32; i++) await page.evaluate(() => window.openStream());
    // Occupy both callback workers; HTTP and binary upload must still finish.
    await page.evaluate(() => window.streams.slice(0, 2).forEach(ws => ws.send("hold")));
    for (let i = 0; i < 40 && (await gauges()).activeHandlers !== 2; i++) await delay(25);
    assert.equal((await gauges()).activeHandlers, 2, "Both stream workers must be occupied");
    const started = performance.now();
    await page.getByRole("button", { name: "Increment", exact: true }).click();
    await page.getByText("Count: 1", { exact: true }).waitFor();
    const chooser = page.waitForEvent("filechooser");
    await page.getByRole("button", { name: "Upload transport fixture", exact: true }).click();
    await (
        await chooser
    ).setFiles({
        name: "fixture.bin",
        mimeType: "application/octet-stream",
        buffer: Buffer.alloc(65536, 9)
    });
    await page.getByText("Uploads: 1", { exact: true }).waitFor();
    report.httpAndUploadMillis = performance.now() - started;
    await delay(100);
    assert.equal(
        (await gauges()).activeHandlers,
        2,
        "HTTP/upload must finish BEFORE stream timeout"
    );
    report.checks.push("32 WSS connections + blocked handlers: HTTP event and binary upload pass");
    // Surviving idle connections must answer heartbeat through the real browser.
    await page.waitForFunction(
        () => window.streams.filter(ws => ws.readyState === WebSocket.OPEN).length === 30
    );
    assert.equal(
        await page.evaluate(
            () => window.streams.filter(ws => ws.readyState === WebSocket.OPEN).length
        ),
        30
    );
    report.checks.push("heartbeat keeps idle browsers alive; stuck callbacks time out");
    for (const socket of tunnels) socket.destroy();
    await page.waitForFunction(() =>
        window.streams.every(ws => ws.readyState === WebSocket.CLOSED)
    );
    await page.evaluate(() => window.openStream());
    const echo = await page.evaluate(
        () =>
            new Promise(resolve => {
                const ws = window.streams.at(-1);
                ws.onmessage = event => resolve(event.data);
                ws.send("after reconnect");
            })
    );
    assert.equal(echo, "after reconnect");
    blocked = true;
    assert.equal(
        await page.evaluate(() =>
            window.openStream().then(
                () => false,
                () => true
            )
        ),
        true
    );
    await page.getByRole("button", { name: "Increment", exact: true }).click();
    await page.getByText("Count: 2", { exact: true }).waitFor();
    blocked = false;
    report.checks.push(
        "proxy disconnect/reconnect; HTTP remains usable when upgrades are unavailable"
    );
    const cookies = (await context.cookies()).map(item => `${item.name}=${item.value}`).join("; ");
    const silent = new WebSocket(
        `${origin.replace("https:", "wss:")}/widgets-stream`,
        "widgets.stream.v1",
        { rejectUnauthorized: false, autoPong: false, headers: { Origin: origin, Cookie: cookies } }
    );
    const closed = new Promise(resolve => silent.once("close", resolve));
    silent.on("error", error => {
        throw error;
    });
    await new Promise(resolve => silent.once("open", resolve));
    silent.send(JSON.stringify(auth));
    await Promise.race([
        closed,
        delay(7000).then(() => {
            throw new Error("Missing pong did not expire");
        })
    ]);
    report.checks.push("unresponsive peer expires without pong");
    await page.getByRole("button", { name: "Revoke session", exact: true }).click();
    await page.waitForFunction(() =>
        window.streams.every(ws => ws.readyState === WebSocket.CLOSED)
    );
    for (let i = 0; i < 60; i++) {
        try {
            const gauges = JSON.parse(await readFile(`${output}/metrics.json`, "utf8"));
            if (Object.values(gauges).every(value => value === 0)) {
                report.finalGauges = gauges;
                break;
            }
        } catch {
            /* atomicity is not required for a private sampling file */
        }
        await delay(100);
    }
    assert.ok(report.finalGauges, "Resources did not drain after revocation");
    report.checks.push("revocation closes active sockets; all resource gauges return to zero");
    report.chromium = browser.version();
    report.passed = true;
} finally {
    await writeFile(`${output}/report.json`, JSON.stringify(report, null, 2));
    await writeFile(`${output}/server.log`, errors);
    await browser?.close();
    for (const socket of tunnels) socket.destroy();
    proxy.closeAllConnections();
    await new Promise(resolve => proxy.close(resolve));
    if (child.exitCode === null) {
        const exited = new Promise(resolve => child.once("exit", resolve));
        child.kill();
        await exited;
    }
}
console.log(JSON.stringify(report, null, 2));
