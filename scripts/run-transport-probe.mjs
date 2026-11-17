import { spawn, execFileSync } from "node:child_process";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { createServer } from "node:net";
import { dirname, resolve, join, delimiter } from "node:path";
import { fileURLToPath } from "node:url";
import { cpus, totalmem } from "node:os";
import { setTimeout as delay } from "node:timers/promises";
import { performance } from "node:perf_hooks";
import process from "node:process";
import assert from "node:assert/strict";
import { chromium } from "@playwright/test";
import { gateway } from "./transport/gateway.mjs";
import { install, contract } from "./transport/browser-contract.mjs";
import { distribution } from "./load/stats.mjs";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const output = join(root, "target/transport-probe");
await mkdir(output, { recursive: true });
const javaPath = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, "bin/java") : "java";
const classpath = [
    join(root, "target/test-classes"),
    join(root, "target/classes"),
    (await readFile(join(root, "target/runtime-classpath.txt"), "utf8")).trim()
].join(delimiter);
const port = await new Promise(resolve => {
    const s = createServer();
    s.listen(0, "127.0.0.1", () => {
        const p = s.address().port;
        s.close(() => resolve(p));
    });
});
const backend = `http://127.0.0.1:${port}`;
let child, browser, bridge;
let errors = "";
async function stop() {
    if (!child || child.exitCode !== null) return;
    const exited = new Promise(resolve => child.once("exit", resolve));
    child.kill();
    await exited;
}
async function start() {
    await stop();
    child = spawn(
        javaPath,
        ["-Xmx1g", "-cp", classpath, "com.kniazkov.widgets.e2e.TransportTestServer", String(port)],
        { stdio: ["ignore", "ignore", "pipe"] }
    );
    child.stderr.on("data", data => {
        errors = (errors + data).slice(-64000);
    });
    for (let i = 0; i < 100; i++) {
        try {
            if ((await fetch(backend, { signal: AbortSignal.timeout(500) })).ok) return;
        } catch {
            /* startup */
        }
        await delay(100);
    }
    throw new Error("Java fixture did not start");
}
const report = {
    commit: execFileSync("git", ["rev-parse", "HEAD"], { cwd: root, encoding: "utf8" }).trim(),
    environment: {
        cpu: cpus()[0].model,
        processors: cpus().length,
        memoryBytes: totalmem(),
        node: process.version
    },
    scope: "Loopback WS-to-HTTP laboratory bridge, not production push or WSS/TLS benchmark",
    runs: []
};
try {
    await start();
    bridge = await gateway(backend);
    browser = await chromium.launch();
    report.environment.chromium = browser.version();
    for (let repetition = 1; repetition <= 2; repetition++) {
        for (const mode of ["http", "ws-prototype"]) {
            await start();
            const context = await browser.newContext();
            try {
                const page = await context.newPage();
                page.setDefaultTimeout(15000);
                await page.goto(bridge.origin);
                const button = page.getByRole("button", { name: "Increment", exact: true });
                await button.waitFor();
                const connectStarted = performance.now();
                await install(page, mode, root);
                const connectMs = performance.now() - connectStarted;
                const before = { ...bridge.stats },
                    timings = [];
                for (let i = 1; i <= 100; i++) {
                    const began = performance.now();
                    await button.click();
                    await page.getByText("Count: " + i, { exact: true }).waitFor();
                    timings.push(performance.now() - began);
                }
                const traffic = Object.fromEntries(
                    Object.keys(before).map(k => [k, bridge.stats[k] - before[k]])
                );
                const uploadBefore = bridge.stats.httpPost;
                const chooserPromise = page.waitForEvent("filechooser");
                await page
                    .getByRole("button", { name: "Upload transport fixture", exact: true })
                    .click();
                const chooser = await chooserPromise;
                await chooser.setFiles({
                    name: "probe.bin",
                    mimeType: "application/octet-stream",
                    buffer: Buffer.alloc(65536, 9)
                });
                await page.getByText("Uploads: 1", { exact: true }).waitFor();
                assert(bridge.stats.httpPost > uploadBefore, "Binary upload did not use HTTP");
                const cases = await contract(page, browser, bridge.origin, mode, root, start);
                report.runs.push({
                    mode,
                    repetition,
                    connectMs,
                    eventToDomMs: distribution(timings),
                    traffic,
                    binaryHttpFallback: true,
                    cases
                });
                console.log(JSON.stringify(report.runs.at(-1)));
                await writeFile(join(output, "report.json"), JSON.stringify(report, null, 2));
            } finally {
                await context.close();
            }
        }
    }
    report.success = true;
} catch (error) {
    report.success = false;
    report.failure = error.message;
    throw error;
} finally {
    await browser?.close();
    await bridge?.close();
    await stop();
    await writeFile(join(output, "report.json"), JSON.stringify(report, null, 2) + "\n");
    await writeFile(join(output, "server.log"), errors);
}
