import process from "node:process";
import { spawn, spawnSync, execFileSync } from "node:child_process";
import { createServer } from "node:net";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { join, dirname, resolve, delimiter } from "node:path";
import { fileURLToPath } from "node:url";
import { cpus, totalmem, platform, release } from "node:os";
import { performance } from "node:perf_hooks";
import { setTimeout as delay } from "node:timers/promises";
import assert from "node:assert/strict";
import { distribution, pool } from "./load/stats.mjs";
import { protocol } from "./load/protocol.mjs";
import { profile, names } from "./load/profiles.mjs";
import { browserProbe, historyProbe } from "./load/browser.mjs";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const args = process.argv.slice(2);
function option(name, fallback) {
    return args.find(a => a.startsWith(name + "="))?.slice(name.length + 1) || fallback;
}
const counts = option("--clients", "1,100,1000").split(",").map(Number);
const profiles = option("--profiles", names.join(",")).split(",");
const repeats = Number(option("--repeats", "2"));
const concurrency = Number(option("--concurrency", "16"));
const samples = Number(option("--dom-samples", "100"));
assert(counts.every(n => Number.isInteger(n) && n > 0 && n <= 1000));
assert(profiles.every(p => names.includes(p)));
assert(Number.isInteger(repeats) && repeats >= 1 && repeats <= 10);
assert(Number.isInteger(concurrency) && concurrency >= 1 && concurrency <= 64);
assert(Number.isInteger(samples) && samples >= 1 && samples <= 1000);
const output = resolve(option("--output", join(root, "target/load-results")));
await mkdir(output, { recursive: true });
const report = {
    schema: 1,
    commit: execFileSync("git", ["rev-parse", "HEAD"], { cwd: root, encoding: "utf8" }).trim(),
    dirty:
        execFileSync("git", ["status", "--porcelain"], { cwd: root, encoding: "utf8" }).trim()
            .length > 0,
    environment: {
        platform: platform(),
        release: release(),
        cpu: cpus()[0]?.model,
        processors: cpus().length,
        memoryBytes: totalmem(),
        node: process.version
    },
    configuration: {
        counts,
        profiles,
        repeats,
        concurrency,
        domSamples: samples,
        javaHeap: "1g",
        sampleIntervalMs: 200
    },
    runs: []
};
const port = await new Promise((accept, reject) => {
    const server = createServer();
    server.on("error", reject);
    server.listen(0, "127.0.0.1", () => {
        const port = server.address().port;
        server.close(error => (error ? reject(error) : accept(port)));
    });
});
const journal = join(output, "metrics.jsonl");
const dependencies = (await readFile(join(root, "target/runtime-classpath.txt"), "utf8")).trim();
const javaPath = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, "bin/java") : "java";
const javaVersion = spawnSync(javaPath, ["-version"], { encoding: "utf8" });
assert.equal(javaVersion.status, 0, "Java unavailable");
report.environment.java = javaVersion.stderr.trim();
const java = spawn(
    javaPath,
    [
        "-Xmx1g",
        "-cp",
        [join(root, "target/test-classes"), join(root, "target/classes"), dependencies].join(
            delimiter
        ),
        "com.kniazkov.widgets.e2e.LoadTestServer",
        String(port),
        journal
    ],
    { stdio: ["ignore", "ignore", "pipe"] }
);
let serverErrors = "";
java.stderr.on("data", data => {
    serverErrors = (serverErrors + data.toString()).slice(-64000);
});
java.on("error", error => {
    serverErrors += error.message;
});
const origin = `http://127.0.0.1:${port}`;
let api, browser;
async function records() {
    const text = await readFile(journal, "utf8");
    return text.split("\n").slice(0, -1).filter(Boolean).map(JSON.parse);
}
async function settled() {
    const previous = (await records()).length;
    for (let attempt = 0; attempt < 100; attempt++) {
        await delay(100);
        const values = await records();
        const last = values.at(-1);
        if (
            values.length > previous &&
            last.pages === 0 &&
            last.uploads === 0 &&
            last.uploadProcessing === 0 &&
            last.uploadQueued === 0
        ) {
            assert.equal(last.widgets, 0);
            assert.equal(last.pendingUpdates, 0);
            assert.equal(last.bufferedUpdates, 0);
            assert.equal(last.activeOperations, 0);
            assert.equal(last.rootWaiters, 0);
            assert.equal(last.channelWaiters, 0);
            assert.equal(last.channelActiveOperations, 0);
            return values;
        }
    }
    throw new Error("Resources did not return to zero after stopping load");
}
try {
    for (let i = 0; i < 100; i++) {
        try {
            api = await protocol(origin);
            break;
        } catch {
            await delay(100);
        }
    }
    assert(api, "Test server did not start");
    if (args.includes("--browser")) {
        const { chromium } = await import("@playwright/test");
        browser = await chromium.launch();
        report.environment.chromium = browser.version();
        report.history = await historyProbe(browser, origin);
    }
    await settled();
    for (let repetition = 1; repetition <= repeats; repetition++) {
        for (const count of counts) {
            for (const name of profiles) {
                const initial = await records();
                const before = initial.at(-1);
                const meter = { requests: 0, requestBytes: 0, responseBytes: 0, httpMs: [] };
                const started = performance.now();
                api.measure(meter);
                const details = await profile(
                    name,
                    count,
                    api,
                    concurrency,
                    browser
                        ? clients =>
                              browserProbe(browser, origin, api, clients, concurrency, samples)
                        : null
                );
                api.measure(null);
                const all = await settled(),
                    after = all.at(-1);
                const observed = all.slice(initial.length);
                const peak = Object.fromEntries(
                    [
                        "pages",
                        "widgets",
                        "pendingUpdates",
                        "pendingUpdateBytes",
                        "bufferedUpdates",
                        "bufferedUpdateBytes",
                        "uploads",
                        "uploadReservedBytes",
                        "uploadProcessing",
                        "uploadQueued",
                        "heapUsedBytes",
                        "activeOperations",
                        "rootWaiters",
                        "channelWaiters",
                        "channelActiveOperations"
                    ].map(key => [key, Math.max(0, ...observed.map(s => s[key]))])
                );
                assert(peak.pages <= details.serverPageLimit, "Live page limit exceeded");
                assert(peak.uploadProcessing <= 1, "Upload worker limit exceeded");
                assert(peak.uploadQueued <= 1, "Upload queue limit exceeded");
                const result = {
                    profile: name,
                    clients: count,
                    repetition,
                    details,
                    requests: meter.requests,
                    requestBytes: meter.requestBytes,
                    responseBytes: meter.responseBytes,
                    httpMs: distribution(meter.httpMs),
                    eventToDomMs: distribution(details.eventToDomMs || []),
                    wallMs: performance.now() - started,
                    cpuMs: (after.processCpuNanos - before.processCpuNanos) / 1e6,
                    gcCount: after.gcCount - before.gcCount,
                    gcMs: after.gcMillis - before.gcMillis,
                    rootLockWaitMs: (after.rootLockWaitNanos - before.rootLockWaitNanos) / 1e6,
                    channelLockWaitMs:
                        (after.channelLockWaitNanos - before.channelLockWaitNanos) / 1e6,
                    peak,
                    stopped: after
                };
                report.runs.push(result);
                console.log(
                    JSON.stringify({
                        profile: name,
                        clients: count,
                        repetition,
                        requests: result.requests,
                        httpP95: result.httpMs.p95,
                        domP95: result.eventToDomMs.p95,
                        cpuMs: result.cpuMs,
                        stoppedPages: after.pages
                    })
                );
                await writeFile(
                    join(output, "report.json"),
                    JSON.stringify(report, null, 2) + "\n"
                );
            }
        }
    }
    // Provisional investigation budgets: largest observed value plus the measured repeat spread.
    // They are not hard CI gates or confidence intervals derived from two observations.
    report.budgets = [];
    for (const name of profiles)
        for (const count of counts) {
            const runs = report.runs.filter(r => r.profile === name && r.clients === count);
            if (runs.length < 2) continue;
            for (const metric of ["cpuMs", "httpP95", "domP95"]) {
                const values = runs
                    .map(r =>
                        metric === "httpP95"
                            ? r.httpMs.p95
                            : metric === "domP95"
                              ? r.eventToDomMs.p95
                              : r.cpuMs
                    )
                    .filter(v => v !== null);
                if (values.length < 2) continue;
                const min = Math.min(...values),
                    max = Math.max(...values);
                report.budgets.push({
                    profile: name,
                    clients: count,
                    metric,
                    min,
                    max,
                    investigationThreshold: max + (max - min)
                });
            }
        }
    report.success = true;
} catch (error) {
    report.success = false;
    report.failure = error.message;
    throw error;
} finally {
    try {
        api?.measure(null);
        if (api) await pool([...api.live], concurrency, c => api.kill(c));
        await browser?.close();
    } finally {
        java.kill();
        await writeFile(join(output, "report.json"), JSON.stringify(report, null, 2) + "\n");
        await writeFile(join(output, "server.log"), serverErrors);
    }
}
