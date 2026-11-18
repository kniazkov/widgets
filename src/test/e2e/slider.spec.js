import { test, expect } from "@playwright/test";

function controls(page) {
    return {
        slider: page.getByRole("slider", { name: "Длина браслета", exact: true }),
        confirmed: page.getByRole("slider", { name: "Подтверждённая длина" })
    };
}

test("exact keys, accessible units, limits and a reactive server reset", async ({ page }) => {
    await page.goto("/all-widgets");
    const { slider, confirmed } = controls(page);
    await expect(confirmed).toBeDisabled();
    await expect(slider).toHaveAttribute("aria-valuemin", "14.00");
    await expect(slider).toHaveAttribute("aria-valuemax", "22.00");
    for (const [key, value] of [
        ["ArrowRight", "18.75"],
        ["ArrowDown", "18.50"],
        ["Home", "14.00"],
        ["PageUp", "14.75"],
        ["End", "22.00"],
        ["PageDown", "21.25"],
        ["ArrowLeft", "21.00"],
        ["ArrowUp", "21.25"]
    ]) {
        await slider.press(key);
        await expect(confirmed).toHaveAttribute("aria-valuenow", value);
        await expect(slider).toHaveAttribute("aria-valuetext", value + " см");
        await expect(slider).toBeFocused();
    }
    await page.getByRole("button", { name: "Сбросить длину" }).click();
    await expect(slider).toHaveAttribute("aria-valuenow", "18.50");
});

test("mouse previews stay local, release commits and pointercancel rolls back", async ({
    page
}) => {
    await page.goto("/all-widgets");
    const { slider, confirmed } = controls(page);
    await slider.scrollIntoViewIfNeeded();
    const box = await slider.boundingBox();
    const y = box.y + box.height / 2;
    for (const cancel of [true, false]) {
        await slider.evaluate(node =>
            node.addEventListener(
                "pointerdown",
                e => {
                    node.dataset.testPointer = String(e.pointerId);
                },
                { once: true }
            )
        );
        await page.mouse.move(box.x + box.width * 0.56, y);
        await page.mouse.down();
        await page.mouse.move(box.x + box.width - 1, y, { steps: 12 });
        await expect(slider).toHaveAttribute("aria-valuenow", "22.00");
        await expect(confirmed).toHaveAttribute("aria-valuenow", "18.50");
        if (cancel) {
            await slider.evaluate(node =>
                node.dispatchEvent(
                    new PointerEvent("pointercancel", {
                        pointerId: Number(node.dataset.testPointer),
                        bubbles: true
                    })
                )
            );
            await expect(slider).toHaveAttribute("aria-valuenow", "18.50");
        }
        await page.mouse.up();
        await expect(confirmed).toHaveAttribute("aria-valuenow", cancel ? "18.50" : "22.00");
    }
});

test.describe("mobile", () => {
    test.use({ viewport: { width: 360, height: 740 }, hasTouch: true });
    test("a touch selects a decimal value and keeps its visible unit", async ({ page }) => {
        await page.goto("/all-widgets");
        const { slider, confirmed } = controls(page);
        await slider.scrollIntoViewIfNeeded();
        const box = await slider.boundingBox();
        await slider.tap({ position: { x: box.width - 2, y: box.height / 2 } });
        await expect(confirmed).toHaveAttribute("aria-valuetext", "22.00 см");
        await expect(slider.locator("..").getByText("22.00 см")).toBeVisible();
        expect(await slider.locator("..").evaluate(el => el.scrollWidth <= el.clientWidth)).toBe(
            true
        );
    });
});
