import { test, expect } from "@playwright/test";

for (const touch of [false, true]) {
    test.describe(touch ? "touch labels" : "mouse labels", () => {
        test.use({ hasTouch: touch, viewport: { width: 390, height: 844 } });
        test("caption actions, radio exclusion and reactive disabled styling", async ({ page }) => {
            await page.goto("/labeled-choices");
            const news = page.getByText("Receive news", { exact: true });
            const delivery = page.getByText("Delivery", { exact: true });
            const pickup = page.getByText("Pickup", { exact: true });
            const icons = page.locator("img");
            await expect(icons).toHaveCount(3);
            const activate = locator => (touch ? locator.tap() : locator.click());
            await activate(news);
            await expect(icons.nth(0)).toHaveJSProperty("_selected", true);
            await activate(icons.nth(0));
            await expect(icons.nth(0)).toHaveJSProperty("_selected", false);
            await activate(pickup);
            await expect(icons.nth(1)).toHaveJSProperty("_selected", false);
            await expect(icons.nth(2)).toHaveJSProperty("_selected", true);
            await activate(pickup);
            await expect(icons.nth(2)).toHaveJSProperty("_selected", true);
            const normal = await news.evaluate(el => getComputedStyle(el).color);
            await activate(page.getByRole("button", { name: "Toggle disabled" }));
            for (const caption of [news, delivery, pickup]) {
                await expect(caption).toHaveCSS("color", "rgb(128, 128, 128)");
            }
            await activate(news);
            await activate(delivery);
            await expect(icons.nth(0)).toHaveJSProperty("_selected", false);
            await expect(icons.nth(2)).toHaveJSProperty("_selected", true);
            await activate(page.getByRole("button", { name: "Toggle disabled" }));
            await expect(news).toHaveCSS("color", normal);
            await activate(delivery);
            await expect(icons.nth(1)).toHaveJSProperty("_selected", true);
        });
    });
}
