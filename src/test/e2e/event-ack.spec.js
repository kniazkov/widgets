import { test, expect } from "@playwright/test";

// Inspect the real transport; runtime functions intentionally remain private to each page.
function eventsIn(request) {
    const match = request.postData()?.match(/name="events"\r\n\r\n([^\r]+)\r\n/);
    return request.method() === "POST" && match ? JSON.parse(match[1]) : [];
}

function deferred() {
    let resolve;
    const promise = new Promise(done => {
        resolve = done;
    });
    return { promise, resolve };
}

test("a click queued before removal becomes obsolete and does not block the next action", async ({
    page
}) => {
    await page.goto("/event-ack");
    const remove = page.getByRole("button", { name: "Remove action", exact: true });
    const action = page.getByRole("button", { name: "Count action", exact: true });
    await expect(remove).toBeVisible();
    const firstProcessed = deferred();
    const deliverFirst = deferred();
    const packets = [];
    const responses = [];
    await page.route("**/*", async route => {
        const events = eventsIn(route.request());
        if (!events.length) return route.continue();
        packets.push(events);
        const response = await route.fetch();
        responses.push(await response.json());
        if (packets.length === 1) {
            firstProcessed.resolve();
            await deliverFirst.promise;
        }
        await route.fulfill({ response });
    });
    try {
        await remove.click();
        await firstProcessed.promise;
        // Java has removed the widget; the first response has not reached the browser yet.
        await remove.click();
        await action.click();
    } finally {
        deliverFirst.resolve();
    }
    await expect(page.getByText("Accepted actions: 1", { exact: true })).toBeVisible();
    await expect(remove).toHaveCount(0);
    expect(responses[1].eventResults.map(result => result.status)).toEqual(["obsolete", "applied"]);
    expect(responses[1].lastEvent).toBe(packets[1].at(-1).id);
    // More than the full 250+500ms retry budget: no repeated event traffic may remain.
    await page.waitForTimeout(1000);
    expect(packets).toHaveLength(2);
    await action.click();
    await expect(page.getByText("Accepted actions: 2", { exact: true })).toBeVisible();
});

test("a lost reply and a double click preserve both actions without repeating controllers", async ({
    page
}) => {
    await page.goto("/event-ack");
    const action = page.getByRole("button", { name: "Count action", exact: true });
    await expect(action).toBeVisible();
    const packets = [];
    const responses = [];
    let lost = false;
    await page.route("**/*", async route => {
        const events = eventsIn(route.request());
        if (!events.length) return route.continue();
        packets.push(events);
        const response = await route.fetch();
        responses.push(await response.json());
        if (!lost) {
            lost = true;
            // The server ran the controller, but its ACK and UI updates are lost.
            await route.abort("failed");
            return;
        }
        await route.fulfill({ response });
    });
    await action.dblclick();
    await expect(page.getByText("Accepted actions: 2", { exact: true })).toBeVisible();
    expect(packets).toHaveLength(2);
    expect(packets[1][0].id).toBe(packets[0][0].id);
    expect(responses[1].eventResults.map(result => result.status)).toEqual(["obsolete", "applied"]);
    await action.click();
    await expect(page.getByText("Accepted actions: 3", { exact: true })).toBeVisible();
});

test("three stalled ACKs back off and rebuild the same page without replaying uncertain input", async ({
    page
}) => {
    await page.goto("/event-ack?keep=42");
    const action = page.getByRole("button", { name: "Count action", exact: true });
    await expect(action).toBeVisible();
    const oldId = await action.evaluate(element => element._id);
    const times = [];
    const packets = [];
    let recreations = 0;
    await page.route("**/*", async route => {
        if (route.request().postData()?.includes('name="action"\r\n\r\nnew instance')) {
            recreations++;
        }
        const events = eventsIn(route.request());
        if (!events.length || events[0].widget !== oldId) return route.continue();
        times.push(Date.now());
        packets.push(events);
        if (packets.length > 3) return route.continue();
        const response = await route.fetch();
        const body = await response.json();
        // Simulate an old/broken endpoint that responds successfully without advancing ACK.
        await route.fulfill({ response, json: { ...body, lastEvent: "#?" } });
    });
    await action.click();
    await expect.poll(() => recreations).toBe(1);
    await expect.poll(() => action.evaluate(element => element._id)).not.toBe(oldId);
    await expect(page.getByText("Accepted actions: 1", { exact: true })).toBeVisible();
    expect(page.url()).toContain("/event-ack?keep=42");
    expect(packets).toHaveLength(3);
    expect(packets.every(packet => packet[0].id === packets[0][0].id)).toBe(true);
    expect(times[1] - times[0]).toBeGreaterThanOrEqual(200);
    expect(times[2] - times[1]).toBeGreaterThanOrEqual(450);
    await action.click();
    await expect(page.getByText("Accepted actions: 2", { exact: true })).toBeVisible();
    await page.waitForTimeout(1000);
    expect(recreations).toBe(1);
});
