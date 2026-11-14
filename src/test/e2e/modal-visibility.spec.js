import { test, expect } from "@playwright/test";

test.describe("ModalPopup visibility lifecycle", () => {
    test("hide and ancestor hide release the page and reopen with the same backdrop", async ({
        page
    }) => {
        await page.goto("/all-widgets");
        const open = page.getByRole("button", { name: "Open lifecycle modal" });
        const clickPage = page.getByRole("button", { name: "Count page click" });
        const outer = page.getByText("Lifecycle modal", { exact: true });
        const backdrop = page.locator(".popup-backdrop");
        await expect(backdrop).toBeHidden();
        for (const [index, action] of ["Hide lifecycle modal", "Hide modal ancestor"].entries()) {
            await open.click();
            await expect(outer).toBeVisible();
            await expect(backdrop).toBeVisible();
            await expect(backdrop).toHaveCSS("background-color", "rgba(30, 64, 175, 0.44)");
            await page.getByRole("button", { name: action }).click();
            await expect(outer).toBeHidden();
            await expect(backdrop).toBeHidden();
            await clickPage.click();
            await expect(page.getByText(`Page clicks: ${index + 1}`)).toBeVisible();
        }
    });

    test.describe("nested modal with touch", () => {
        test.use({ viewport: { width: 390, height: 844 }, hasTouch: true });

        test("nested backdrop covers the viewport; touch closes only the top modal", async ({
            page
        }) => {
            await page.goto("/all-widgets");
            const outer = page.getByText("Lifecycle modal", { exact: true });
            const inner = page.getByText("Nested lifecycle modal", { exact: true });
            const backdrop = page.locator(".popup-backdrop");
            await page.getByRole("button", { name: "Open lifecycle modal" }).tap();
            await page.getByRole("button", { name: "Open nested modal" }).tap();
            await expect(inner).toBeVisible();
            await expect(backdrop).toHaveCount(2);
            const innerBackdrop = backdrop.nth(1);
            await expect(innerBackdrop).toBeVisible();
            expect(await innerBackdrop.boundingBox()).toEqual({
                x: 0,
                y: 0,
                width: 390,
                height: 844
            });
            expect(await innerBackdrop.evaluate(el => document.elementFromPoint(5, 5) === el)).toBe(
                true
            );
            await page.touchscreen.tap(5, 5);
            await expect(inner).toHaveCount(0);
            await expect(outer).toBeVisible();
            await expect(backdrop).toHaveCount(1);
            await page.getByRole("button", { name: "Hide lifecycle modal" }).tap();
            await page.getByRole("button", { name: "Count page click" }).tap();
            await expect(page.getByText("Page clicks: 1")).toBeVisible();
        });
    });
});
