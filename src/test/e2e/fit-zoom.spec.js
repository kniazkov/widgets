import { expect, test } from "@playwright/test";

test.use({ hasTouch: true });

for (const [width, height] of [
    [2400, 1200],
    [120, 240],
    [1000, 1000]
]) {
    test(`fits ${width}x${height} original and refits after rotation`, async ({ page }) => {
        await page.setViewportSize({ width: 390, height: 844 });
        await page.goto(`/fitted-zoom?width=${width}&height=${height}`);
        const zoom = page.locator('[data-zoom="true"]');
        const image = zoom.locator("img");
        const stage = zoom.locator(":scope > span");
        await expect
            .poll(() => image.evaluate(el => el.complete && el.naturalWidth > 0))
            .toBe(true);
        const geometry = () =>
            zoom.evaluate(el => {
                const v = el.getBoundingClientRect(),
                    i = el.querySelector("img").getBoundingClientRect();
                return {
                    side: v.width,
                    width: i.width,
                    height: i.height,
                    x: i.x - v.x,
                    y: i.y - v.y,
                    viewportHeight: v.height
                };
            });
        const check = async side => {
            await expect
                .poll(async () => (await geometry()).width)
                .toBeCloseTo((side * width) / Math.max(width, height), 0);
            const g = await geometry();
            expect(g.height).toBeCloseTo((side * height) / Math.max(width, height), 0);
            expect(g.x).toBeCloseTo((side - g.width) / 2, 0);
            expect(g.y).toBeCloseTo((side - g.height) / 2, 0);
            expect(g.side).toBeCloseTo(side, 0);
            expect(g.viewportHeight).toBeCloseTo(side, 0);
        };
        await check(390);
        await zoom.hover();
        await page.mouse.wheel(0, -350);
        await expect
            .poll(async () => (await geometry()).width)
            .toBeGreaterThan((390 * width) / Math.max(width, height));
        await page.mouse.wheel(0, 2000);
        await check(390);
        await page.setViewportSize({ width: 844, height: 390 });
        await check(390);
        await page.setViewportSize({ width: 1000, height: 600 });
        await check(600);
        const client = await page.context().newCDPSession(page);
        const r = await zoom.boundingBox();
        await client.send("Input.dispatchTouchEvent", {
            type: "touchStart",
            touchPoints: [
                { x: r.x + 250, y: r.y + 300, id: 1 },
                { x: r.x + 350, y: r.y + 300, id: 2 }
            ]
        });
        await client.send("Input.dispatchTouchEvent", {
            type: "touchMove",
            touchPoints: [
                { x: r.x + 200, y: r.y + 300, id: 1 },
                { x: r.x + 400, y: r.y + 300, id: 2 }
            ]
        });
        await client.send("Input.dispatchTouchEvent", { type: "touchEnd", touchPoints: [] });
        await expect
            .poll(() => stage.evaluate(el => new DOMMatrix(getComputedStyle(el).transform).a))
            .toBeCloseTo(1200 / Math.max(width, height), 1);
        const before = await stage.getAttribute("style");
        await page.mouse.move(r.x + 300, r.y + 300);
        await page.mouse.down();
        await page.mouse.move(r.x + 250, r.y + 250, { steps: 5 });
        await page.mouse.up();
        expect(await stage.getAttribute("style")).not.toBe(before);
        await page.touchscreen.tap(5, 5);
        await expect(page.locator(".modal-popup")).toHaveCount(0);
        await expect(page.locator(".popup-backdrop")).toHaveCount(0);
    });
}
