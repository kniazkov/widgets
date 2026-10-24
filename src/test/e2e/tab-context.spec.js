import { test, expect } from "@playwright/test";

async function identity(page, label) {
    return page.getByText(new RegExp(`^${label}: `)).textContent();
}

async function open(page) {
    await page.goto("/tab-context");
    await expect(page.getByText("Mode: normal", { exact: true })).toBeVisible();
}

test("internal history shares a context but revocation cannot be undone by Back", async ({
    page
}) => {
    await open(page);
    const tab = await identity(page, "Tab");
    await page.getByText("Enter tab mode", { exact: true }).click();
    await expect(page.getByText("Mode: operator", { exact: true })).toBeVisible();
    await page.getByText("Next tab page", { exact: true }).click();
    await expect(page.getByText("Mode: operator", { exact: true })).toBeVisible();
    expect(await identity(page, "Tab")).toBe(tab);
    await page.goBack();
    await expect(page.getByText("Mode: operator", { exact: true })).toBeVisible();
    await page.goForward();
    await page.getByText("Revoke tab mode", { exact: true }).click();
    await expect(page.getByText("Mode: normal", { exact: true })).toBeVisible();
    expect(await identity(page, "Tab")).not.toBe(tab);
    await page.goBack();
    await expect(page.getByText("Mode: normal", { exact: true })).toBeVisible();
    expect(await identity(page, "Tab")).not.toBe(tab);
});

test("reload and an opener's copied storage never inherit a mode", async ({ page, context }) => {
    await open(page);
    const tab = await identity(page, "Tab");
    const browser = await identity(page, "Browser");
    await page.getByText("Enter tab mode", { exact: true }).click();
    await expect(page.getByText("Mode: operator", { exact: true })).toBeVisible();
    await page.evaluate(value => sessionStorage.setItem("tabId", value), tab.slice(5));
    const popupPromise = context.waitForEvent("page");
    await page.evaluate(() => window.open("/tab-context?tabId=" + sessionStorage.getItem("tabId")));
    const popup = await popupPromise;
    await expect(popup.getByText("Mode: normal", { exact: true })).toBeVisible();
    expect(await popup.evaluate(() => sessionStorage.getItem("tabId"))).toBe(tab.slice(5));
    expect(await identity(popup, "Tab")).not.toBe(tab);
    expect(await identity(popup, "Browser")).toBe(browser);
    await expect(page.getByText("Mode: operator", { exact: true })).toBeVisible();
    await popup.close();
    await page.reload();
    await expect(page.getByText("Mode: normal", { exact: true })).toBeVisible();
    expect(await identity(page, "Tab")).not.toBe(tab);
    expect(await identity(page, "Browser")).toBe(browser);
});

test("two independent tabs and a reconnect preserve separate identities", async ({
    page,
    context
}) => {
    await open(page);
    const tab = await identity(page, "Tab");
    const browser = await identity(page, "Browser");
    const other = await context.newPage();
    await open(other);
    expect(await identity(other, "Browser")).toBe(browser);
    expect(await identity(other, "Tab")).not.toBe(tab);
    await page.getByText("Enter tab mode", { exact: true }).click();
    await expect(page.getByText("Mode: operator", { exact: true })).toBeVisible();
    await expect(other.getByText("Mode: normal", { exact: true })).toBeVisible();
    await context.setOffline(true);
    await page.waitForTimeout(3000);
    await context.setOffline(false);
    await page.getByText("Next tab page", { exact: true }).click();
    await expect(page.getByText("Mode: operator", { exact: true })).toBeVisible();
    expect(await identity(page, "Tab")).toBe(tab);
    await other.close();
});
