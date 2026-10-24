import { expect, test } from "@playwright/test";

test("countdown expires locally, replaces deadline and survives navigation cache and reattachment", async ({
    page
}) => {
    await page.goto("/countdown");
    const timer = page.getByRole("timer");
    await expect(timer).toHaveText(/00:0[1-9]|00:10/);
    await page.getByRole("button", { name: "Set past deadline" }).click();
    await expect(timer).toHaveText("00:00");
    await page.getByRole("button", { name: "Set one hour" }).click();
    await expect(timer).toHaveText(/1:00:0[01]|59:59/);
    const original = await timer.elementHandle();
    await page.getByRole("link", { name: "Next countdown page" }).click();
    await expect(page).toHaveURL(/countdown-next$/);
    await expect(timer).toBeVisible();
    await page.goBack();
    await expect(page).toHaveURL(/countdown$/);
    await expect.poll(() => original.evaluate(node => node.isConnected)).toBe(true);
    await expect(timer).toHaveText(/1:00:0[01]|59:\d{2}/);
    await page.getByRole("button", { name: "Remove countdown" }).click();
    await expect(timer).toHaveCount(0);
    await page.getByRole("button", { name: "Attach countdown" }).click();
    await expect(timer).toBeVisible();
    await page.getByRole("button", { name: "Restart 10 seconds" }).click();
    await expect(timer).toHaveText(/00:0[1-9]|00:10/);
    // Keep the ordinary transport alive, but verify no countdown events are sent at zero.
    const payloads = [];
    page.on("request", request => {
        if (request.method() === "POST") payloads.push(request.postData() || "");
    });
    await expect(timer).toHaveText("00:00", { timeout: 15000 });
    expect(payloads.length).toBeGreaterThan(0);
    // Two retained runtimes synchronize every 2.5s; digits add no requests or events.
    expect(payloads.length).toBeLessThanOrEqual(12);
    expect(payloads.every(body => /name="events"\r\n\r\n\[\]/.test(body))).toBe(true);
    await original.dispose();
});
