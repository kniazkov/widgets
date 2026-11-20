import { expect, test } from "@playwright/test";

test("a bounded burst rebuilds a coherent DOM without replaying the same save", async ({
    page
}) => {
    await page.goto(process.env.WIDGETS_UPLOAD_LIMIT_BASE_URL + "/protocol-backpressure");
    const snapshot = page.getByText(/^Snapshot /);
    const confirmed = page.getByText(/^Confirmed /);
    await expect(snapshot).toBeVisible();
    const version = await snapshot.textContent();
    const initial = Number((await confirmed.textContent()).split(" ").at(-1));
    await page.getByRole("button", { name: "Save normally" }).click();
    await expect(confirmed).toHaveText("Confirmed " + (initial + 1));
    await page.getByRole("button", { name: "Save and refresh under pressure" }).click();
    await expect(snapshot).not.toHaveText(version);
    await expect(confirmed).toHaveText("Confirmed " + (initial + 2));
    await expect(page.getByText("Current page")).toBeVisible();
    await expect(page.getByText(/^Update /)).toHaveCount(0);

    await page.getByRole("button", { name: "Save normally" }).click();
    await expect(confirmed).toHaveText("Confirmed " + (initial + 3));
});
