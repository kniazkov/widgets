import { test, expect } from "@playwright/test";

test("a combined batch replaces a subtree, skips its old target and preserves subsequent actions", async ({
    page
}) => {
    await page.goto("/live-event-targets");
    const button = name => page.getByRole("button", { name, exact: true });
    await expect(button("Hold response")).toBeVisible();
    let release;
    let ready;
    const held = new Promise(resolve => {
        release = resolve;
    });
    const processed = new Promise(resolve => {
        ready = resolve;
    });
    const packets = [];
    const responses = [];
    await page.route("**/*", async route => {
        const match = route
            .request()
            .postData()
            ?.match(/name="events"\r\n\r\n([^\r]+)\r\n/);
        const events = match ? JSON.parse(match[1]) : [];
        if (!events.length) return route.continue();
        packets.push(events);
        const response = await route.fetch();
        responses.push(await response.json());
        if (packets.length === 1) {
            ready();
            await held;
        }
        await route.fulfill({ response });
    });
    try {
        await button("Hold response").click();
        await processed;
        // Keep one request in flight so the next three real clicks form a single packet.
        await button("Replace tree").click();
        await button("Old target").click();
        await button("Following action").click();
    } finally {
        release();
    }
    await expect(page.getByText("Tree actions: 1", { exact: true })).toBeVisible();
    expect(packets[1]).toHaveLength(3);
    expect(responses[1].eventResults.map(result => result.status)).toEqual([
        "applied",
        "obsolete",
        "applied"
    ]);
    expect(responses[1].lastEvent).toBe(packets[1].at(-1).id);
    await expect(button("Old target")).toHaveCount(0);
    await button("New target").click();
    await expect(page.getByText("Tree actions: 11", { exact: true })).toBeVisible();
    await button("Following action").click();
    await expect(page.getByText("Tree actions: 12", { exact: true })).toBeVisible();
    await expect(page.locator("#client-error-overlay")).toHaveCount(0);
});
