import { expect, test } from "@playwright/test";

test("mouse reorder reaches Java and preserves the child DOM nodes", async ({ page }) => {
    await page.goto("/gestures");
    const sortable = page.locator('[data-sortable="true"]');
    const first = sortable.locator(":scope > *").nth(0);
    const last = sortable.locator(":scope > *").nth(2);
    await expect(first).toBeVisible();
    await first.evaluate(element => {
        window.originalSortChild = element;
    });
    const a = await first.boundingBox();
    const b = await last.boundingBox();
    await page.mouse.move(a.x + 20, a.y + 40);
    await page.mouse.down();
    await page.mouse.move(b.x + b.width - 10, b.y + 40, { steps: 8 });
    await page.mouse.up();
    await expect(page.getByText("First: Sort card 2", { exact: true })).toBeVisible();
    expect(
        await sortable.evaluate(element => element.lastElementChild === window.originalSortChild)
    ).toBe(true);
    await sortable.locator(":scope > *").nth(0).focus();
    await page.keyboard.press("Alt+ArrowRight");
    await expect(page.getByText("First: Sort card 3", { exact: true })).toBeVisible();
    await expect(sortable.locator(":scope > *").nth(1)).toBeFocused();
});

test("drag follows the pointer above stationary siblings, then animates their new positions", async ({
    page
}) => {
    await page.goto("/gestures");
    const sortable = page.locator('[data-sortable="true"]');
    await page.getByRole("button", { name: "Slow sorting", exact: true }).click();
    await expect.poll(() => sortable.evaluate(element => element._animationDuration)).toBe(600);
    const first = sortable.locator(":scope > *").nth(0);
    const second = sortable.locator(":scope > *").nth(1);
    const last = sortable.locator(":scope > *").nth(2);
    const a = await first.boundingBox();
    const b = await second.boundingBox();
    const c = await last.boundingBox();
    await page.mouse.move(a.x + 20, a.y + 40);
    await page.mouse.down();
    await page.mouse.move(c.x + 90, c.y + 40, { steps: 8 });
    expect((await first.boundingBox()).x).toBeCloseTo(c.x + 70, 0);
    expect((await second.boundingBox()).x).toBeCloseTo(b.x, 0);
    expect((await last.boundingBox()).x).toBeCloseTo(c.x, 0);
    await expect(first).toHaveCSS("z-index", "1000");
    await expect(first).toHaveCSS("opacity", "1");
    await page.mouse.up();
    const durations = await sortable.evaluate(element => {
        window.dropAnimations = Array.from(element.children).flatMap(child =>
            child.getAnimations()
        );
        for (const animation of window.dropAnimations) animation.pause();
        return window.dropAnimations.map(animation => animation.effect.getTiming().duration);
    });
    expect(durations).toEqual([600, 600, 600]);
    await expect(page.getByText("First: Sort card 2", { exact: true })).toBeVisible();
    expect(
        await sortable.evaluate(element => {
            const active = Array.from(element.children).flatMap(child => child.getAnimations());
            return window.dropAnimations.every(animation => active.includes(animation));
        })
    ).toBe(true);
    await page.evaluate(() => {
        for (const animation of window.dropAnimations) animation.currentTime = 300;
    });
    const moving = await sortable.locator(":scope > *").nth(0).boundingBox();
    expect(moving.x).toBeGreaterThan(a.x);
    expect(moving.x).toBeLessThan(b.x);
    await page.evaluate(() => {
        for (const animation of window.dropAnimations) animation.finish();
    });
    await expect
        .poll(() =>
            sortable.evaluate(
                element =>
                    Array.from(element.children).flatMap(child => child.getAnimations()).length
            )
        )
        .toBe(0);
    await expect(sortable.locator(":scope > *").nth(2)).toHaveCSS("z-index", "auto");
    await page.getByRole("button", { name: "Instant sorting", exact: true }).click();
    await expect.poll(() => sortable.evaluate(element => element._animationDuration)).toBe(0);
    await sortable.locator(":scope > *").nth(0).focus();
    await page.keyboard.press("Alt+ArrowRight");
    expect(
        await sortable.evaluate(
            element => Array.from(element.children).flatMap(child => child.getAnimations()).length
        )
    ).toBe(0);
});

test("wheel zoom and mouse pan retain nested button clicks and server reset", async ({ page }) => {
    await page.goto("/gestures");
    const zoom = page.locator('[data-zoom="true"]');
    const stage = zoom.locator(":scope > span");
    await page.getByRole("button", { name: "Zoom child button" }).click();
    await expect(page.getByText("Zoom clicks: 1", { exact: true })).toBeVisible();
    await zoom.hover({ position: { x: 150, y: 100 } });
    await page.mouse.wheel(0, -350);
    await expect(stage).not.toHaveCSS("transform", "matrix(1, 0, 0, 1, 0, 0)");
    const before = await stage.evaluate(element => element.style.transform);
    const rect = await zoom.boundingBox();
    await page.mouse.move(rect.x + 150, rect.y + 100);
    await page.mouse.down();
    await page.mouse.move(rect.x + 100, rect.y + 80, { steps: 5 });
    await page.mouse.up();
    expect(await stage.evaluate(element => element.style.transform)).not.toBe(before);
    await expect(page.getByText("Zoom clicks: 1", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Reset viewport" }).click();
    await expect(stage).toHaveCSS("transform", "matrix(1, 0, 0, 1, 0, 0)");
    await page.getByRole("button", { name: "Zoom child button" }).click();
    await expect(page.getByText("Zoom clicks: 2", { exact: true })).toBeVisible();
});

test("touch reorder and pinch use real browser pointer capture", async ({ browser, baseURL }) => {
    const context = await browser.newContext({
        viewport: { width: 390, height: 844 },
        isMobile: true,
        hasTouch: true
    });
    try {
        const page = await context.newPage();
        const errors = [];
        page.on("pageerror", error => errors.push(error.message));
        await page.goto(`${baseURL}/gestures`);
        const sortable = page.locator('[data-sortable="true"]');
        await expect(sortable).toBeVisible();
        const client = await context.newCDPSession(page);
        const touch = (type, points) =>
            client.send("Input.dispatchTouchEvent", { type, touchPoints: points });
        const first = await sortable.locator(":scope > *").nth(0).boundingBox();
        const last = await sortable.locator(":scope > *").nth(2).boundingBox();
        await touch("touchStart", [{ x: first.x + 20, y: first.y + 40, id: 1 }]);
        await touch("touchMove", [{ x: last.x + 90, y: last.y + 40, id: 1 }]);
        await touch("touchEnd", []);
        await expect(page.getByText("First: Sort card 2", { exact: true })).toBeVisible();
        const zoom = page.locator('[data-zoom="true"]');
        const box = await zoom.boundingBox();
        const stage = zoom.locator(":scope > span");
        await touch("touchStart", [
            { x: box.x + 100, y: box.y + 120, id: 1 },
            { x: box.x + 200, y: box.y + 120, id: 2 }
        ]);
        await touch("touchMove", [
            { x: box.x + 50, y: box.y + 120, id: 1 },
            { x: box.x + 250, y: box.y + 120, id: 2 }
        ]);
        await touch("touchEnd", []);
        await expect(stage).not.toHaveCSS("transform", "matrix(1, 0, 0, 1, 0, 0)");
        expect(
            await stage.evaluate(element => new DOMMatrix(getComputedStyle(element).transform).a)
        ).toBeCloseTo(2, 1);
        expect(errors).toEqual([]);
    } finally {
        await context.close();
    }
});
