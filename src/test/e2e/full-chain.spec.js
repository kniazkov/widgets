import { expect, test } from "@playwright/test";

test("a browser click reaches Java and the resulting update reaches the DOM", async ({ page }) => {
    const pageErrors = [];
    page.on("pageerror", error => pageErrors.push(error.message));

    await page.goto("/");

    await expect(page.getByText("Waiting for browser event", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Run full chain" }).click();
    await expect(page.getByText("Java handled the click", { exact: true })).toBeVisible();
    expect(pageErrors).toEqual([]);
});
