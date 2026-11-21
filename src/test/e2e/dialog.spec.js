import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

const button = (scope, name) => scope.getByRole("button", { name, exact: true });
const dialog = (page, name = "Sample request") => page.getByRole("dialog", { name, exact: true });
test.use({ hasTouch: true });

for (const width of [1280, 390]) {
    test(`dialog validates, guards duplicate Enter, blocks busy close and retries at ${width}px`, async ({
        page
    }) => {
        await page.setViewportSize({ width, height: 800 });
        await page.goto("/all-widgets");
        const open = button(page, "Open long form");
        if (width === 390) await open.tap();
        else await open.click();
        const modal = dialog(page);
        const field = modal.getByRole("textbox", { name: "Sample name", exact: true });
        await field.press("Enter");
        await expect(modal.getByText("Enter a sample name.", { exact: true })).toBeVisible();
        await expect(field).toBeFocused();
        await expect(modal.getByText("Save attempts: 0")).toBeAttached();
        await field.fill("Paper");
        await field.press("Enter");
        await field.press("Enter");
        await expect(button(modal, "Confirm")).toBeDisabled();
        await expect(button(modal, "Cancel")).toBeDisabled();
        await expect(button(modal, "Close dialog")).toBeDisabled();
        await page.keyboard.press("Escape");
        await page.mouse.click(2, 2);
        await expect(modal).toBeVisible();
        await expect(
            modal.getByText("Could not save. Please retry.", { exact: true })
        ).toBeAttached();
        await expect(modal.getByText("Save attempts: 1")).toBeAttached();
        await expect(field).toHaveValue("Paper");
        await expect(button(modal, "Confirm")).toBeEnabled();
        await button(modal, "Confirm").dblclick();
        await expect(modal).toHaveCount(0);
        await expect(page.getByText("Result: CONFIRMED · Paper", { exact: true })).toBeVisible();
        await expect(open).toBeFocused();
    });
}

test("confirmation distinguishes cancel and close, supports Escape and backdrop", async ({
    page
}) => {
    await page.goto("/all-widgets");
    const open = button(page, "Open confirmation");
    await open.click();
    const modal = dialog(page, "Confirm action");
    await expect(modal).toBeVisible();
    const audit = await new AxeBuilder({ page }).include(".widgets-dialog").analyze();
    expect(audit.violations).toEqual([]);
    await button(modal, "Cancel").click();
    await expect(page.getByText("Result: CANCELLED", { exact: true })).toBeVisible();
    await expect(open).toBeFocused();
    await open.press("Enter");
    await button(modal, "Close dialog").click();
    await expect(page.getByText("Result: CLOSED", { exact: true })).toBeVisible();
    await open.click();
    await expect(modal).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(modal).toHaveCount(0);
    await expect(open).toBeFocused();
    await open.click();
    await expect(modal).toBeVisible();
    await page.mouse.click(2, 2);
    await expect(modal).toHaveCount(0);
    await open.click();
    await button(modal, "Confirm").press("Enter");
    await expect(page.getByText("Result: CONFIRMED", { exact: true })).toBeVisible();
});

test("nested dialog returns focus to its launcher then to the original page", async ({ page }) => {
    await page.goto("/all-widgets");
    const open = button(page, "Open long form");
    await open.click();
    const outer = dialog(page);
    const nested = button(outer, "Open nested confirmation");
    await nested.click();
    const inner = dialog(page, "Nested confirmation");
    await expect(inner).toBeVisible();
    await expect(page.getByRole("dialog")).toHaveCount(1);
    await page.keyboard.press("Escape");
    await expect(inner).toHaveCount(0);
    await expect(nested).toBeFocused();
    await button(outer, "Cancel").click();
    await expect(outer).toHaveCount(0);
    await expect(open).toBeFocused();
});

test("long form remains scrollable and focused controls stay inside a panned keyboard viewport", async ({
    page
}) => {
    await page.setViewportSize({ width: 390, height: 700 });
    await page.goto("/all-widgets");
    await button(page, "Open long form").tap();
    const modal = dialog(page);
    await expect(modal).toBeVisible();
    const captionFits = await button(modal, "Open nested confirmation").evaluate(el => {
        const button = el.getBoundingClientRect();
        const range = document.createRange();
        range.selectNodeContents(el);
        const text = range.getBoundingClientRect();
        return text.top >= button.top && text.bottom <= button.bottom;
    });
    expect(captionFits).toBe(true);
    await page.evaluate(() => {
        const viewport = new EventTarget();
        Object.assign(viewport, { width: 390, height: 300, offsetLeft: 0, offsetTop: 100 });
        Object.defineProperty(window, "visualViewport", { configurable: true, value: viewport });
        window.dispatchEvent(new Event("resize"));
    });
    for (let i = 0; i < 13; i++) {
        await page.keyboard.press("Tab");
        const bounds = await modal.evaluate(el => {
            const r = el.getBoundingClientRect();
            const active = document.activeElement.getBoundingClientRect();
            return {
                top: r.top,
                bottom: r.bottom,
                width: r.width,
                scroll: el.scrollHeight > el.clientHeight,
                focusInside:
                    el.contains(document.activeElement) &&
                    active.top >= r.top - 1 &&
                    active.bottom <= r.bottom + 1
            };
        });
        expect(bounds.top).toBeGreaterThanOrEqual(111);
        expect(bounds.bottom).toBeLessThanOrEqual(389);
        expect(bounds.width).toBeLessThanOrEqual(367);
        expect(bounds.scroll).toBe(true);
        expect(bounds.focusInside).toBe(true);
    }
    await page.keyboard.press("Escape");
    await expect(modal).toHaveCount(0);
});
