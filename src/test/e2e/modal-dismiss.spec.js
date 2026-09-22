import { test, expect } from "@playwright/test";

test.use({ hasTouch: true });

for (const input of ["mouse", "touch"]) {
    test(`outside ${input} dismisses only an enabled modal`, async ({ page }) => {
        await page.goto("/");
        const outside = () =>
            input === "touch" ? page.touchscreen.tap(5, 5) : page.mouse.click(5, 5);
        await page.getByRole("button", { name: "Show modal message" }).click();
        await outside();
        await expect(page.getByText("Modal message", { exact: true })).toBeVisible();
        await page.getByRole("button", { name: "Close modal message" }).click();
        for (let repeat = 0; repeat < 2; repeat++) {
            await page.getByRole("button", { name: "Show dismissible modal" }).click();
            const message = page.getByText("Dismissible modal", { exact: true });
            await expect(message).toBeVisible();
            await message.click();
            await expect(message).toBeVisible();
            const toggle = page.getByRole("button", { name: "Toggle outside closing" });
            const modal = message.locator("xpath=ancestor::*[contains(@class,'popup')][1]");
            await toggle.click();
            await expect.poll(() => modal.evaluate(el => el._closeOnOutsideClick)).toBe(false);
            await outside();
            await expect(message).toBeVisible();
            await toggle.click();
            await expect.poll(() => modal.evaluate(el => el._closeOnOutsideClick)).toBe(true);
            await outside();
            await expect(message).toHaveCount(0);
            await expect(page.locator(".popup-backdrop")).toHaveCount(0);
        }
    });
}
