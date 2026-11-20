import { test, expect } from "@playwright/test";

const outerName = "Размер списка коллекций";
const innerName = "Высота карточки";

async function middle(handle) {
    await handle.scrollIntoViewIfNeeded();
    const box = await handle.boundingBox();
    return { x: box.x + box.width / 2, y: box.y + box.height / 2 };
}

test("nested separators support keys, container resize and shared reset", async ({ page }) => {
    await page.setViewportSize({ width: 1100, height: 850 });
    await page.goto("/all-widgets");
    const outer = page.getByRole("separator", { name: outerName });
    const inner = page.getByRole("separator", { name: innerName });
    await expect(outer).toHaveAttribute("aria-orientation", "vertical");
    await outer.press("Shift+ArrowRight");
    await expect(outer).toHaveAttribute("aria-valuenow", "48");
    await expect(inner).toHaveAttribute("aria-valuenow", "55");
    await inner.press("End");
    await expect(inner).toHaveAttribute("aria-valuenow", "80");
    await outer.press("Home");
    await expect(outer).toHaveAttribute("aria-valuenow", "20");
    await page.setViewportSize({ width: 360, height: 740 });
    await expect(outer).toHaveAttribute("aria-orientation", "horizontal");
    await expect(outer).toBeFocused();
    await outer.press("ArrowDown");
    await expect(outer).toHaveAttribute("aria-valuenow", "21");
    await page.getByRole("button", { name: "Сбросить размеры" }).click();
    await expect(outer).toHaveAttribute("aria-valuenow", "38");
    await expect(inner).toHaveAttribute("aria-valuenow", "55");
    await page.getByText("Зафиксировать размеры", { exact: true }).click();
    await expect(outer).toHaveAttribute("aria-disabled", "true");
});

test("captured drag commits once, cancellation and disposal discard previews", async ({ page }) => {
    await page.goto("/split-lifecycle");
    const handle = page.getByRole("separator", { name: "Divider" });
    const at = await middle(handle);
    await page.mouse.move(at.x, at.y);
    await page.mouse.down();
    await page.mouse.move(at.x + 2000, at.y + 20);
    await page.mouse.up();
    await expect(page.getByText("85", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Reset" }).click();
    await expect(handle).toHaveAttribute("aria-valuenow", "60");
    await page.getByRole("textbox").focus();
    await page.setViewportSize({ width: 820, height: 740 });
    await expect(page.getByRole("textbox")).toBeFocused();
    for (const remove of [false, true]) {
        const point = await middle(handle);
        await handle.evaluate(node =>
            node.addEventListener(
                "pointerdown",
                event => {
                    node.dataset.testPointer = String(event.pointerId);
                },
                { once: true }
            )
        );
        await page.mouse.move(point.x, point.y);
        await page.mouse.down();
        await page.mouse.move(point.x - 120, point.y);
        await expect(handle).not.toHaveAttribute("aria-valuenow", "60");
        await expect(page.getByText("60", { exact: true })).toBeVisible();
        if (remove) {
            await page.getByRole("button", { name: "Remove" }).evaluate(node => node.click());
            await expect(handle).toHaveCount(0);
        } else {
            await handle.evaluate(node =>
                node.dispatchEvent(
                    new PointerEvent("pointercancel", {
                        bubbles: true,
                        pointerId: Number(node.dataset.testPointer)
                    })
                )
            );
            await expect(handle).toHaveAttribute("aria-valuenow", "60");
            await expect(handle).toBeFocused();
        }
        await page.mouse.up();
        await expect(page.getByText("60", { exact: true })).toBeVisible();
    }
});

test.describe("mobile touch", () => {
    test.use({ viewport: { width: 360, height: 740 }, hasTouch: true });
    test("stacked handle takes focus and resizes on a touchscreen", async ({
        page,
        browserName
    }) => {
        await page.goto("/split-lifecycle");
        const handle = page.getByRole("separator", { name: "Divider" });
        const point = await middle(handle);
        await handle.tap();
        await expect(handle).toBeFocused();
        if (browserName === "chromium") {
            const session = await page.context().newCDPSession(page);
            for (const [type, dy] of [
                ["touchStart", 0],
                ["touchMove", 45],
                ["touchEnd", 0]
            ]) {
                await session.send("Input.dispatchTouchEvent", {
                    type,
                    touchPoints: type === "touchEnd" ? [] : [{ x: point.x, y: point.y + dy }]
                });
            }
            const value = Number(await handle.getAttribute("aria-valuenow"));
            expect(value).toBeGreaterThan(40);
            await expect(page.getByText(String(value), { exact: true })).toBeVisible();
        }
    });
});
