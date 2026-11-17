import assert from "node:assert/strict";
import { performance } from "node:perf_hooks";
import { pool } from "./stats.mjs";

export async function browserProbe(browser, origin, api, clients, concurrency, samples) {
    const page = await browser.newPage();
    await instrument(page);
    let running = true,
        failure;
    const background = (async () => {
        try {
            while (running) await pool(clients, concurrency, c => api.sync(c));
        } catch (error) {
            failure = error;
        }
    })();
    try {
        await page.goto(origin + "/load");
        const increment = page.getByRole("button", { name: "Increment", exact: true });
        await increment.waitFor();
        const values = [];
        for (let i = 1; i <= samples; i++) {
            const started = performance.now();
            await increment.click();
            await page.getByText("Count: " + i, { exact: true }).waitFor();
            values.push(performance.now() - started);
        }
        return values;
    } finally {
        running = false;
        await background;
        await cleanup(page);
        if (failure) throw failure;
    }
}
export async function historyProbe(browser, origin) {
    const page = await browser.newPage();
    await instrument(page);
    await page.clock.install({ time: new Date("2026-11-17T00:00:00Z") });
    await page.clock.pauseAt(new Date("2026-11-17T01:00:00Z"));
    try {
        await page.goto(origin + "/load");
        await page.getByRole("button", { name: "Increment", exact: true }).waitFor();
        await page.evaluate(() => {
            window.originalLoadRoot = document.querySelector(".widgets-page");
        });
        await page.getByRole("link", { name: "Next load page" }).click();
        await page.getByRole("button", { name: "Increment", exact: true }).click();
        await page.getByText("Count: 1", { exact: true }).waitFor();
        await page.goBack();
        await page.getByText("Count: 1", { exact: true }).waitFor();
        assert(
            await page.evaluate(
                () => document.querySelector(".widgets-page") === window.originalLoadRoot
            )
        );
        await page.goForward();
        await page.getByText("Count: 1", { exact: true }).waitFor();
        await page.clock.fastForward(121000);
        await page.goBack();
        await page.getByText("Count: 1", { exact: true }).waitFor();
        assert(
            await page.evaluate(
                () => document.querySelector(".widgets-page") !== window.originalLoadRoot
            )
        );
        return { retainedDom: true, backgroundState: true, ttlRebuilt: true };
    } finally {
        await cleanup(page);
    }
}
async function instrument(page) {
    await page.addInitScript(() => {
        window.loadChannels = [];
        const send = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.send = function (body) {
            if (body instanceof FormData && body.get("action") === "new instance") {
                this.addEventListener("load", () => {
                    const value = JSON.parse(this.responseText);
                    if (value.id) window.loadChannels.push(value);
                });
            }
            return send.call(this, body);
        };
    });
}
async function cleanup(page) {
    try {
        await page.evaluate(async () => {
            await Promise.all(
                window.loadChannels.map(
                    c =>
                        new Promise(resolve => {
                            sendRequest({ action: "kill", client: c.id }, resolve, "post", null, {
                                token: c.channelToken,
                                epoch: c.serverId,
                                updateEpoch: c.updateEpoch
                            });
                        })
                )
            );
        });
    } finally {
        await page.close();
    }
}
