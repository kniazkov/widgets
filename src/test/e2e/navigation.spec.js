import { expect, test } from "@playwright/test";

function trackCreations(page) {
    const creations = [];
    page.on("request", request => {
        if (new URL(request.url()).searchParams.get("action") === "new instance") {
            creations.push(request.url());
        }
    });
    return creations;
}

test("Back and Forward retain the catalog DOM, local input and scroll", async ({ page }) => {
    const creations = trackCreations(page);
    await page.goto("/catalog?filter=blue");
    await page.getByRole("textbox").fill("My unfinished filter");
    await page.evaluate(() => {
        window.__catalogInput = document.querySelector("input");
        window.scrollTo(0, 920);
    });
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(920);
    await page.getByRole("link", { name: "Open product" }).evaluate(link => link.click());
    await expect(page.getByText("Product 42", { exact: true })).toBeVisible();
    await page.goBack();
    await expect(page.getByRole("textbox")).toHaveValue("My unfinished filter");
    expect(
        await page.evaluate(() => document.querySelector("input") === window.__catalogInput)
    ).toBe(true);
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(920);
    expect(creations).toHaveLength(2);
    await page.goForward();
    await expect(page.getByText("Product 42", { exact: true })).toBeVisible();
    expect(creations).toHaveLength(2);
});

test("server navigation and authentication invalidation rebuild only invalidated entries", async ({
    page
}) => {
    const creations = trackCreations(page);
    await page.goto("/catalog");
    await page.getByRole("textbox").fill("Before logout");
    await page.getByRole("button", { name: "Navigate from Java" }).click();
    await expect(page.getByText("Product 43", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Clear cached pages" }).click();
    await expect
        .poll(() => page.evaluate(() => localStorage.getItem("widgetsPageCacheInvalidation")))
        .toBeTruthy();
    await page.goBack();
    await expect(page.getByRole("textbox")).toHaveValue("");
    expect(creations).toHaveLength(3);
});

test("middle-click opens the native link in a new tab", async ({ page, context }) => {
    await page.goto("/catalog");
    const opened = context.waitForEvent("page");
    await page.getByRole("link", { name: "Open product" }).click({ button: "middle" });
    const product = await opened;
    await expect(product.getByText("Product 42", { exact: true })).toBeVisible();
    expect(new URL(page.url()).pathname).toBe("/catalog");
    await product.close();
});
