import { expect, test } from "@playwright/test";

function field(request, key) {
    return request.method() === "POST"
        ? request.postData()?.match(new RegExp(`name="${key}"\\r\\n\\r\\n([^\\r]+)\\r\\n`))?.[1]
        : null;
}

const fixture = "/connection-recovery?mode=draft";
const ready = page => page.getByText("Connection recovery fixture", { exact: true });
const countButton = page => page.getByRole("button", { name: "Count recovery action" });
const count = (page, n) => page.getByText(`Accepted actions: ${n}`, { exact: true });
const indicator = page => page.locator("#connection-terminated-overlay");
const fatal = page => page.locator("#client-error-overlay");

test("503 during bootstrap recovers without a permanent client error", async ({ page }) => {
    let attempts = 0;
    await page.route("**/*", async route => {
        if (field(route.request(), "action") === "new instance" && ++attempts === 1) {
            return route.fulfill({ status: 503, body: "temporary gateway" });
        }
        await route.continue();
    });
    await page.goto(fixture);
    await expect(ready(page)).toBeVisible();
    await expect(fatal(page)).toHaveCount(0);
    expect(attempts).toBe(2);
});

test("502 then 504 after execution retries the same event exactly once", async ({ page }) => {
    await page.goto(fixture);
    await expect(ready(page)).toBeVisible();
    const packets = [];
    await page.route("**/*", async route => {
        const events = field(route.request(), "events");
        if (!events || events === "[]") return route.continue();
        packets.push(JSON.parse(events).map(event => event.id));
        if (packets.length === 1) return route.fulfill({ status: 502 });
        if (packets.length === 2) {
            const applied = await route.fetch();
            expect(applied.ok()).toBe(true);
            return route.fulfill({ status: 504 });
        }
        await route.continue();
    });
    await countButton(page).click();
    await expect(count(page, 1)).toBeVisible();
    expect(packets).toEqual([packets[0], packets[0], packets[0]]);
    await expect(fatal(page)).toHaveCount(0);
    await countButton(page).click();
    await expect(count(page, 2)).toBeVisible();
});

test("an XHR timeout leaves the queued event intact", async ({ page }) => {
    await page.goto(fixture);
    await expect(ready(page)).toBeVisible();
    const packets = [];
    page.on("request", request => {
        const events = field(request, "events");
        if (events && events !== "[]") packets.push(JSON.parse(events).map(event => event.id));
    });
    await page.getByRole("button", { name: "Count delayed recovery action" }).click();
    await expect(count(page, 1)).toBeVisible({ timeout: 20000 });
    expect(packets).toHaveLength(2);
    expect(packets[1]).toEqual(packets[0]);
});

test("offline recovery removes the indicator and keeps draft and Back navigation", async ({
    page,
    context
}) => {
    await page.goto(fixture);
    await expect(ready(page)).toBeVisible();
    const draft = page.getByRole("textbox");
    await draft.fill("draft stays here");
    await context.setOffline(true);
    try {
        await countButton(page).click();
        await expect(indicator(page)).toBeVisible({ timeout: 12000 });
        await expect(draft).toHaveValue("draft stays here");
        await expect(count(page, 0)).toBeVisible();
    } finally {
        await context.setOffline(false);
    }
    await expect(indicator(page)).toHaveCount(0);
    await expect(count(page, 1)).toBeVisible();
    await page.getByRole("link", { name: "Open recovery catalog" }).click();
    await expect(page).toHaveURL(/\/catalog$/);
    await page.goBack();
    await expect(page).toHaveURL(/\/connection-recovery\?mode=draft$/);
    await expect(draft).toHaveValue("draft stays here");
    await expect(count(page, 1)).toBeVisible();
});

for (const [status, headers, reason] of [
    [401, {}, "access"],
    [403, {}, "access"],
    [200, { "X-Widgets-Protocol-Version": "42" }, "protocol"],
    [500, { "X-Widgets-Protocol-Error": "application" }, "application"]
]) {
    test(`terminal response ${status} stops all synchronization: ${reason}`, async ({ page }) => {
        await page.goto(fixture);
        await expect(ready(page)).toBeVisible();
        let attempts = 0;
        await page.route("**/*", async route => {
            if (field(route.request(), "action") !== "synchronize") return route.continue();
            attempts++;
            await route.fulfill({ status, headers, body: "{}" });
        });
        await countButton(page).click();
        await expect(fatal(page)).toHaveText("Client Error");
        await expect(fatal(page)).toHaveAttribute("data-reason", reason);
        await expect(indicator(page)).toHaveCount(0);
        await page.waitForTimeout(2750);
        expect(attempts).toBe(1);
    });
}

test("an expired session gets only one fresh document attempt before stopping", async ({
    page
}) => {
    let attempts = 0;
    await page.route("**/*", async route => {
        if (field(route.request(), "action") !== "new instance") return route.continue();
        attempts++;
        await route.fulfill({
            status: 403,
            contentType: "application/json",
            body: '{"sessionExpired":true}'
        });
    });
    await page.goto(fixture).catch(error => {
        if (!/interrupted|NS_BINDING_ABORTED|ERR_ABORTED/.test(error.message)) throw error;
    });
    await expect(fatal(page)).toHaveAttribute("data-reason", "session");
    await expect(ready(page)).toHaveCount(0);
    await page.waitForTimeout(2750);
    expect(attempts).toBe(2);
    await expect(page).toHaveURL(/\/connection-recovery\?mode=draft$/);
});
