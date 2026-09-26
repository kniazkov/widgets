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

for (const width of [320, 390, 1280]) {
    test(`long captions stay beside checkbox and radio at ${width}px`, async ({ page }) => {
        await page.setViewportSize({ width, height: 844 });
        await page.goto("/labeled-choices");
        const captions = page.locator(".widgets-labeled-choice-row");
        await expect(captions).toHaveCount(3);
        for (const text of [
            "Brass with gold plating and brass with rhodium plating, a long description",
            "VeryLongUnbrokenCaption".repeat(12)
        ]) {
            const geometry = await captions.evaluateAll(
                (rows, value) =>
                    rows.map(row => {
                        const control = row.firstElementChild;
                        const label = row.children[1];
                        const caption = label.querySelector("span");
                        const originalWidth = control.getBoundingClientRect().width;
                        caption.textContent = value;
                        const icon = control.getBoundingClientRect();
                        const bounds = label.getBoundingClientRect();
                        const range = document.createRange();
                        range.selectNodeContents(caption);
                        const lines = [...range.getClientRects()];
                        return {
                            originalWidth,
                            iconWidth: icon.width,
                            iconRight: icon.right,
                            labelLeft: bounds.left,
                            labelRight: bounds.right,
                            lineLefts: lines.map(line => line.left),
                            scrollWidth: row.scrollWidth,
                            width: row.clientWidth
                        };
                    }),
                text
            );
            for (const item of geometry) {
                expect(item.iconWidth).toBe(item.originalWidth);
                expect(item.labelLeft).toBeGreaterThanOrEqual(item.iconRight + 5);
                expect(item.labelRight).toBeLessThanOrEqual(width);
                expect(item.scrollWidth).toBeLessThanOrEqual(item.width + 1);
                for (const left of item.lineLefts)
                    expect(left).toBeGreaterThanOrEqual(item.labelLeft - 1);
            }
        }
    });
}
