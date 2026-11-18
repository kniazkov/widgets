import { test, expect } from "@playwright/test";

test("minimal consumer serves its packaged public document and handles clicks", async ({ page }) => {
    await page.goto("http://127.0.0.1:17801");
    await expect(page.getByRole("heading", { name: "Minimal" })).toBeVisible();
    await page.getByRole("button", { name: "Test interaction" }).click();
    await expect(page.getByText("Clicked", { exact: true })).toBeVisible();
});

test("full consumer starts the packaged AllWidgets gallery", async ({ page }) => {
    await page.goto("http://127.0.0.1:17802");
    await expect(page).toHaveTitle("All Widgets — Component Gallery");
    await page.getByRole("button", { name: "Change separator color" }).click();
});
