import { spawn, execFileSync } from "node:child_process";
import { readFile, mkdir, writeFile, rm } from "node:fs/promises";
import { createServer as tlsServer } from "node:https";
import { request } from "node:http";
import { createServer } from "node:net";
import { join, delimiter } from "node:path";
import { setTimeout as delay } from "node:timers/promises";
import process from "node:process";
import assert from "node:assert/strict";
import { chromium } from "@playwright/test";

// A TLS reverse proxy only: WebSocket frames travel directly to the Java adapter.
const output = "target/deployment-probe";
await mkdir(output, { recursive: true });
const failures = {};
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
const nodes = {};
for (const name of ["A", "B"])
    nodes[name] = { http: await freePort(), ws: await freePort(), child: null };
let migration = false,
    wrongHttp = false,
    wrongWs = false;
const tunnels = new Set();
function owner(req) {
    const sticky =
        req.headers.cookie?.match(/(?:^|;\s*)__Host-widgets-node=([AB])(?:;|$)/)?.[1] || "A";
    return migration && sticky === "A" ? "B" : sticky;
}
function running(node) {
    return node.child && node.child.exitCode === null && node.child.signalCode === null;
}
function alive(node) {
    return node.ready && running(node);
}
const proxy = tlsServer(
    { key: await readFile(`${output}/key.pem`), cert: await readFile(`${output}/cert.pem`) },
    (req, res) => {
        const name = wrongHttp && req.method === "POST" ? "B" : owner(req),
            node = nodes[name];
        if (!alive(node)) {
            failures[503] = (failures[503] || 0) + 1;
            res.writeHead(503);
            res.end();
            return;
        }
        const upstream = request(
            {
                hostname: "127.0.0.1",
                port: node.http,
                path: req.url,
                method: req.method,
                headers: req.headers
            },
            reply => {
                const headers = { ...reply.headers };
                if (req.method === "GET" && req.url === "/") {
                    headers["set-cookie"] = [
                        ...(headers["set-cookie"] || []),
                        `__Host-widgets-node=${name}; Path=/; Secure; HttpOnly; SameSite=Strict`
                    ];
                }
                res.writeHead(reply.statusCode, headers);
                reply.pipe(res);
            }
        );
        upstream.on("error", () => {
            res.writeHead(503);
            res.end();
        });
        req.pipe(upstream);
    }
);
proxy.on("upgrade", (req, socket, head) => {
    const node = nodes[wrongWs ? "B" : owner(req)];
    if (!alive(node) || req.url !== "/widgets-stream") {
        socket.end("HTTP/1.1 503 Unavailable\r\nContent-Length: 0\r\n\r\n");
        return;
    }
    const upstream = request({
        hostname: "127.0.0.1",
        port: node.ws,
        path: req.url,
        headers: req.headers
    });
    upstream.on("upgrade", (res, remote, rest) => {
        socket.write(
            `HTTP/1.1 101 Switching Protocols\r\n${Object.entries(res.headers)
                .map(([key, value]) => `${key}: ${value}\r\n`)
                .join("")}\r\n`
        );
        if (head.length) remote.write(head);
        if (rest.length) socket.write(rest);
        tunnels.add(socket);
        socket.on("close", () => {
            tunnels.delete(socket);
            remote.destroy();
        });
        remote.on("close", () => socket.destroy());
        socket.on("error", () => remote.destroy());
        remote.on("error", () => socket.destroy());
        socket.pipe(remote).pipe(socket);
    });
    upstream.on("response", reply => {
        socket.end(`HTTP/1.1 ${reply.statusCode} Rejected\r\nContent-Length: 0\r\n\r\n`);
        reply.resume();
    });
    upstream.on("error", () => socket.destroy());
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
let browser,
    errors = "";
async function stop(name) {
    const child = nodes[name].child;
    nodes[name].ready = false;
    if (!running(nodes[name])) return;
    const exited = new Promise(resolve => child.once("exit", resolve));
    child.kill("SIGKILL");
    await exited;
}
async function metrics(name) {
    for (let i = 0; i < 40; i++) {
        try {
            return JSON.parse(await readFile(`${output}/${name}.json`, "utf8"));
        } catch {
            await delay(25);
        }
    }
    throw new Error("Missing node journal " + name);
}
async function start(name) {
    await stop(name);
    await rm(`${output}/${name}.json`, { force: true });
    const node = nodes[name];
    node.child = spawn(
        java,
        [
            "-Xmx512m",
            "-cp",
            classpath,
            "com.kniazkov.widgets.e2e.DeploymentTestServer",
            String(node.http),
            String(node.ws),
            origin,
            name,
            `${output}/business.sqlite`,
            `${output}/${name}.json`
        ],
        { stdio: ["ignore", "ignore", "pipe"] }
    );
    node.child.stderr.on("data", data => {
        errors = (errors + data).slice(-64000);
    });
    for (let i = 0; i < 150; i++) {
        if (!running(node)) throw new Error("Node exited: " + name);
        try {
            JSON.parse(await readFile(`${output}/${name}.json`, "utf8"));
            node.ready = true;
            return;
        } catch {
            await delay(100);
        }
    }
    throw new Error("Node startup timed out: " + name);
}
async function waitMetric(name, predicate) {
    for (let i = 0; i < 100; i++) {
        const value = await metrics(name);
        if (predicate(value)) return value;
        await delay(100);
    }
    throw new Error("Node gauge condition failed: " + name + JSON.stringify(await metrics(name)));
}
async function replay(page, packet, oldSecurity) {
    return page.evaluate(
        async ({ packet, oldSecurity }) => {
            const form = new FormData();
            for (const [key, value] of Object.entries(packet.query))
                form.append(key, typeof value === "object" ? JSON.stringify(value) : value);
            const reply = await fetch("/", {
                method: "POST",
                body: form,
                headers: {
                    "X-Widgets-Session": oldSecurity?.session || protocolSession,
                    "X-Widgets-CSRF": oldSecurity?.csrf || protocolCsrf,
                    "X-Widgets-Channel": packet.channel.token,
                    "X-Widgets-Epoch": packet.channel.epoch,
                    "X-Widgets-Update-Epoch": packet.channel.updateEpoch
                }
            });
            return reply.status;
        },
        { packet, oldSecurity }
    );
}
const report = {
    commit: execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim(),
    nodes: 2,
    nativeWss: true,
    tls: true,
    checks: [],
    failures
};
let inspectedPage;
try {
    await rm(`${output}/business.sqlite`, { force: true });
    await start("A");
    await start("B");
    browser = await chromium.launch();
    report.chromium = browser.version();
    const context = await browser.newContext({ ignoreHTTPSErrors: true });
    const page = await context.newPage();
    inspectedPage = page;
    page.setDefaultTimeout(30000);
    let command;
    page.on("websocket", socket =>
        socket.on("framesent", frame => {
            const packet = JSON.parse(frame.payload);
            if (packet.type === "request" && packet.query.events?.length) command = packet;
        })
    );
    await page.goto(origin);
    await page.getByText("Node A", { exact: true }).waitFor();
    await page.waitForFunction(() => documentPushCoordinator?.state === "open");
    await page.getByRole("button", { name: "Demo login", exact: true }).click();
    await page.getByText("Durable count: 0", { exact: true }).waitFor();
    await page.getByRole("button", { name: "Commit operation 1", exact: true }).click();
    await page.getByText("Durable count: 1", { exact: true }).waitFor();
    const oldCommand = command;
    assert.ok(oldCommand?.query.events.length);
    await waitMetric("A", g => g.handlers === 1);
    wrongHttp = true;
    assert.equal(await replay(page, oldCommand), 403);
    wrongHttp = false;
    assert.equal((await metrics("B")).handlers, 0);
    wrongWs = true;
    const rejected = await page.evaluate(
        () =>
            new Promise(resolve => {
                const socket = new WebSocket(
                    location.origin.replace(/^http/, "ws") + "/widgets-stream",
                    "widgets.stream.v1"
                );
                const timer = setTimeout(() => {
                    socket.close();
                    resolve("timeout");
                }, 5000);
                const finish = value => {
                    clearTimeout(timer);
                    socket.close();
                    resolve(value);
                };
                socket.onopen = () => finish("opened");
                socket.onerror = socket.onclose = () => finish("rejected");
            })
    );
    wrongWs = false;
    assert.equal(rejected, "rejected");
    const other = await browser.newContext({ ignoreHTTPSErrors: true });
    await other.addCookies([
        {
            name: "__Host-widgets-node",
            value: "B",
            url: origin,
            secure: true,
            httpOnly: true,
            sameSite: "Strict"
        }
    ]);
    const second = await other.newPage();
    await second.goto(origin);
    await second.getByText("Node B", { exact: true }).waitFor();
    await second.getByText("Signed out", { exact: true }).waitFor();
    assert.equal(await replay(second, oldCommand), 403);
    assert.equal((await metrics("B")).handlers, 0);
    report.checks.push(
        "sticky HTTP/WSS; wrong-node cookie and foreign owner/epoch rejected with 403 before any handler"
    );
    await other.close();
    await stop("A");
    await page.getByRole("button", { name: "Commit operation 2", exact: true }).click();
    await delay(1200);
    assert.ok(failures[503] > 0);
    assert.equal((await metrics("B")).handlers, 0);
    assert.equal((await metrics("B")).count, 1);
    migration = true; // explicit operator failover; never a retry of the old POST on a second node
    await page.getByText("Node B", { exact: true }).waitFor();
    await page.getByText("Signed out", { exact: true }).waitFor();
    await page.getByText("Counter requires login", { exact: true }).waitFor();
    assert.equal((await metrics("B")).handlers, 0);
    await page.getByRole("button", { name: "Demo login", exact: true }).click();
    await page.getByText("Durable count: 1", { exact: true }).waitFor();
    assert.equal(await replay(page, oldCommand), 403);
    await page.getByRole("button", { name: "Commit operation 1", exact: true }).click();
    await waitMetric("B", g => g.handlers === 1);
    assert.equal((await metrics("B")).count, 1);
    await page.getByRole("button", { name: "Commit operation 2", exact: true }).click();
    await page.getByText("Durable count: 2", { exact: true }).waitFor();
    report.checks.push(
        "owner loss returns 503; explicit failover requires fresh authorization, drops uncertain UI events and reconstructs durable state"
    );
    report.checks.push(
        "same durable business operation submitted on both nodes commits once; a new operation commits separately"
    );
    const previousEpoch = command.channel.epoch;
    await start("B");
    await page.getByText("Signed out", { exact: true }).waitFor();
    await page.getByRole("button", { name: "Demo login", exact: true }).click();
    await page.getByText("Durable count: 2", { exact: true }).waitFor();
    await page.waitForFunction(() => documentPushCoordinator?.state === "open");
    await delay(150); // allow the ready -> subscribed exchange to complete
    await page.getByRole("button", { name: "Commit operation 1", exact: true }).click();
    await waitMetric("B", g => g.handlers === 1);
    assert.notEqual(command.channel.epoch, previousEpoch);
    const security = await page.evaluate(() => ({ session: protocolSession, csrf: protocolCsrf }));
    const beforeLogout = command;
    await page.getByRole("button", { name: "Logout", exact: true }).click();
    await page.getByText("Signed out", { exact: true }).waitFor();
    assert.equal(await replay(page, beforeLogout, security), 403);
    assert.equal((await metrics("B")).count, 2);
    report.checks.push(
        "node restart keeps committed data; logout revokes old credentials and native channels"
    );
    await context.close();
    await start("A");
    assert.equal((await metrics("A")).count, 2);
    await waitMetric("B", g => g.connections === 0);
    report.passed = true;
} catch (error) {
    report.failure = error.message;
    try {
        report.page = await inspectedPage.evaluate(() => ({
            path: location.pathname,
            text: document.body.innerText.slice(0, 1500)
        }));
    } catch {
        /* closed document */
    }
    throw error;
} finally {
    await writeFile(`${output}/report.json`, JSON.stringify(report, null, 2));
    await writeFile(`${output}/server.log`, errors);
    await browser?.close();
    for (const socket of tunnels) socket.destroy();
    proxy.closeAllConnections();
    await new Promise(resolve => proxy.close(resolve));
    await stop("A");
    await stop("B");
}
console.log(JSON.stringify(report, null, 2));
