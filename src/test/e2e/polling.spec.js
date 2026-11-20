import { expect, test } from "@playwright/test";

async function instrument(page) {
    await page.addInitScript(() => {
        const send = XMLHttpRequest.prototype.send;
        window.pollingRequests = [];
        window.pollingInFlight = 0;
        XMLHttpRequest.prototype.send = function (body) {
            if (body instanceof FormData) {
                window.pollingInFlight++;
                this.addEventListener("loadend", () => window.pollingInFlight--, { once: true });
                window.pollingRequests.push({
                    action: body.get("action"),
                    client: body.get("client"),
                    at: Date.now()
                });
            }
            return send.call(this, body);
        };
    });
}

test("idle backoff preserves immediate events, background updates, Back and cache TTL", async ({
    page
}, info) => {
    await instrument(page);
    // Install before navigation: an already-running native timer cannot be moved to the fake clock.
    await page.clock.install({ time: new Date("2026-11-17T00:00:00Z") });
    await page.clock.pauseAt(new Date("2026-11-17T01:00:00Z"));
    await page.goto("/polling");
    const button = page.getByRole("button", { name: "Change shared polling value" });
    await expect(button).toBeVisible();
    await page.evaluate(() => {
        window.savedPollingRoot = document.querySelector(".widgets-page");
    });
    await page.getByRole("link", { name: "Next polling page" }).click();
    await expect(button).toBeVisible();
    await page.clock.runFor(2500);
    // Wait for the response to set the next adaptive deadline before advancing again.
    await expect
        .poll(() =>
            page.evaluate(
                () => window.pollingRequests.filter(x => x.action === "synchronize").length
            )
        )
        .toBeGreaterThanOrEqual(4);
    const value = page.getByText(/^(Initial|Updated) polling value/);
    const previous = await value.textContent();
    const started = performance.now();
    await button.click();
    await expect(value).not.toHaveText(previous);
    await info.attach("event-to-DOM.json", {
        body: JSON.stringify({ milliseconds: performance.now() - started }),
        contentType: "application/json"
    });
    await expect.poll(() => page.evaluate(() => window.pollingInFlight)).toBe(0);
    await page.clock.fastForward(30000);
    await expect
        .poll(() => page.evaluate(() => window.savedPollingRoot.textContent))
        .toContain("Updated polling value");
    await page.goBack();
    await expect(value).not.toHaveText(previous);
    expect(
        await page.evaluate(
            () => document.querySelector(".widgets-page") === window.savedPollingRoot
        )
    ).toBe(true);
    const creations = await page.evaluate(
        () => window.pollingRequests.filter(x => x.action === "new instance").length
    );
    expect(creations).toBe(2);
    await page.goForward();
    await expect(button).toBeVisible();
    await page.clock.fastForward(121000);
    await page.goBack();
    await expect(button).toBeVisible();
    expect(
        await page.evaluate(
            () => document.querySelector(".widgets-page") === window.savedPollingRoot
        )
    ).toBe(false);
    expect(
        await page.evaluate(
            () => window.pollingRequests.filter(x => x.action === "new instance").length
        )
    ).toBe(3);
});

test("visibility wake immediately synchronizes a hidden document", async ({ page }) => {
    await instrument(page);
    // Install before navigation: an already-running native timer cannot be moved to the fake clock.
    await page.clock.install({ time: new Date("2026-11-17T00:00:00Z") });
    await page.clock.pauseAt(new Date("2026-11-17T01:00:00Z"));
    await page.goto("/polling");
    await expect(page.getByRole("button", { name: "Change shared polling value" })).toBeVisible();
    await page.evaluate(() => {
        Object.defineProperty(document, "visibilityState", {
            configurable: true,
            get: () => "hidden"
        });
        document.dispatchEvent(new Event("visibilitychange"));
    });
    await page.clock.runFor(2500);
    await expect
        .poll(() =>
            page.evaluate(
                () => window.pollingRequests.filter(x => x.action === "synchronize").length
            )
        )
        .toBe(2);
    await expect.poll(() => page.evaluate(() => window.pollingInFlight)).toBe(0);
    await page.evaluate(() => {
        Object.defineProperty(document, "visibilityState", {
            configurable: true,
            get: () => "visible"
        });
        document.dispatchEvent(new Event("visibilitychange"));
    });
    await page.clock.runFor(1);
    await expect
        .poll(() =>
            page.evaluate(
                () => window.pollingRequests.filter(x => x.action === "synchronize").length
            )
        )
        .toBe(3);
});
