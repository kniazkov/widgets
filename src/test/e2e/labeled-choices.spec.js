import AxeBuilder from "@axe-core/playwright";
import { test, expect } from "@playwright/test";

for (const touch of [false, true]) {
    test.describe(touch ? "touch labels" : "mouse labels", () => {
        test.use({ hasTouch: touch, viewport: { width: 390, height: 844 } });
        test("caption actions, radio exclusion and reactive disabled styling", async ({ page }) => {
            await page.goto("/labeled-choices");
            const news = page.getByText("Receive news", { exact: true });
            const delivery = page.getByText("Delivery", { exact: true });
            const pickup = page.getByText("Pickup", { exact: true });
            const checkbox = page.getByRole("checkbox", { name: "Receive news" });
            const first = page.getByRole("radio", { name: "Delivery" });
            const second = page.getByRole("radio", { name: "Pickup" });
            const checks = [];
            page.on("request", request => {
                const match = request.postData()?.match(/name="events"\r\n\r\n([^\r]+)\r\n/);
                if (match)
                    checks.push(...JSON.parse(match[1]).filter(event => event.type === "check"));
            });
            const activate = locator => (touch ? locator.tap() : locator.click());
            await activate(news);
            await expect(checkbox).toBeChecked();
            await activate(checkbox);
            await expect(checkbox).not.toBeChecked();
            await activate(pickup);
            await expect(first).not.toBeChecked();
            await expect(second).toBeChecked();
            await activate(pickup);
            await expect(second).toBeChecked();
            const normal = await news.evaluate(el => getComputedStyle(el).color);
            await activate(page.getByRole("button", { name: "Toggle disabled" }));
            for (const caption of [news, delivery, pickup]) {
                await expect(caption).toHaveCSS("color", "rgb(128, 128, 128)");
            }
            await expect(checkbox).toBeDisabled();
            await expect(first).toBeDisabled();
            // Physical input reaches the label even when its associated control is disabled.
            for (const caption of [news, delivery]) {
                const bounds = await caption.boundingBox();
                const x = bounds.x + bounds.width / 2;
                const y = bounds.y + bounds.height / 2;
                if (touch) await page.touchscreen.tap(x, y);
                else await page.mouse.click(x, y);
            }
            await expect(checkbox).not.toBeChecked();
            await expect(second).toBeChecked();
            await activate(page.getByRole("button", { name: "Toggle disabled" }));
            await expect(news).toHaveCSS("color", normal);
            await activate(delivery);
            await expect(first).toBeChecked();
            await expect.poll(() => checks.length).toBe(4);
            expect(checks.map(event => event.data.state)).toEqual([true, false, true, true]);
        });
    });
}

for (const width of [320, 390, 1280]) {
    test(`long captions stay beside checkbox and radio at ${width}px`, async ({ page }) => {
        await page.setViewportSize({ width, height: 844 });
        await page.goto("/labeled-choices");
        const captions = page.locator(".widgets-labeled-choice-row");
        await expect(captions).toHaveCount(4);
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

test("native choice accessibility, focus, Space and arrow-only navigation", async ({ page }) => {
    await page.goto("/labeled-choices");
    const box = page.getByRole("checkbox", { name: "Receive news" });
    const delivery = page.getByRole("radio", { name: "Delivery" });
    const unavailable = page.getByRole("radio", { name: "Unavailable option" });
    const pickup = page.getByRole("radio", { name: "Pickup" });
    const toggle = page.getByRole("button", { name: "Toggle disabled" });
    await expect(box).toBeVisible();
    const tree = await page.locator(".widgets-page").ariaSnapshot();
    expect(tree).toContain('checkbox "Receive news"');
    expect(tree).toContain('radio "Delivery" [checked]');
    expect(tree).toContain('radio "Unavailable option" [disabled]');
    await expect(delivery).toBeChecked();
    await expect(unavailable).toBeDisabled();
    const audit = await new AxeBuilder({ page }).include(".widgets-labeled-choice-row").analyze();
    expect(audit.violations).toEqual([]);
    await page.keyboard.press("Tab");
    await expect(box).toBeFocused();
    await expect(box).toHaveCSS("outline-style", "solid");
    await expect(box).toHaveCSS("outline-width", "2px");
    await page.keyboard.press("Space");
    await expect(box).toBeChecked();
    await page.keyboard.press("Tab");
    await expect(delivery).toBeFocused();
    await page.keyboard.press("ArrowRight");
    await expect(pickup).toBeFocused();
    await expect(pickup).toBeChecked();
    await expect(delivery).not.toBeChecked();
    await page.keyboard.press("ArrowDown");
    await expect(delivery).toBeFocused();
    await expect(delivery).toBeChecked();
    await expect(pickup).not.toBeChecked();
    await page.keyboard.press("Tab");
    await expect(toggle).toBeFocused();
    await page.keyboard.press("Space");
    await expect(box).toBeDisabled();
    await expect(delivery).toBeDisabled();
    await expect(pickup).toBeDisabled();
    await expect(box).toBeChecked();
});
