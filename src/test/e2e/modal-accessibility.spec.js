import { expect, test } from "@playwright/test";

const button = (scope, name) => scope.getByRole("button", { name, exact: true });

test("only the top dialog is accessible and keyboard focus cycles inside it", async ({ page }) => {
    await page.goto("/all-widgets");
    const open = button(page, "Open lifecycle modal");
    await open.focus();
    await page.keyboard.press("Enter");
    await expect(button(page, "Hide lifecycle modal")).toBeFocused();
    const outer = page.getByRole("dialog", { name: "Lifecycle modal", exact: true });
    const first = button(outer, "Hide lifecycle modal");
    const nested = button(outer, "Open nested modal");
    await expect(first).toBeFocused();
    await expect(outer).toHaveAttribute("aria-modal", "true");
    await expect(outer).toHaveAccessibleDescription(
        "Use Tab to move between actions; Escape closes this dialog."
    );
    const background = button(page, "Count page click");
    expect(await background.evaluate(el => Boolean(el.closest("[inert]")))).toBe(true);
    await background.evaluate(el => el.focus());
    await expect(first).toBeFocused();
    await page.keyboard.press("Shift+Tab");
    await expect(nested).toBeFocused();
    await page.keyboard.press("Tab");
    await expect(first).toBeFocused();
    await nested.focus();
    await page.keyboard.press("Enter");
    const inner = page.getByRole("dialog", { name: "Nested lifecycle modal", exact: true });
    const close = button(inner, "Close nested modal");
    await expect(close).toBeFocused();
    await expect(outer).toHaveCount(0);
    await expect(inner).toHaveAttribute("aria-modal", "true");
    await expect(inner).toHaveAccessibleDescription("Keyboard focus stays in this nested dialog.");
    await button(inner, "Toggle nested Escape").click();
    await expect.poll(() => inner.evaluate(el => el._closeOnEscape)).toBe(false);
    await page.keyboard.press("Escape");
    await expect(inner).toBeVisible();
    await expect(page.getByRole("dialog")).toHaveCount(1);
    await button(inner, "Toggle nested Escape").click();
    await expect.poll(() => inner.evaluate(el => el._closeOnEscape)).toBe(true);
    await page.keyboard.press("Escape");
    await expect(inner).toHaveCount(0);
    await expect(nested).toBeFocused();
    await page.keyboard.press("Escape");
    await expect(outer).toHaveCount(0);
    await expect(open).toBeFocused();
    await expect(open).toHaveCSS("outline-style", "solid");
    await page.keyboard.press("Enter");
    await expect(first).toBeFocused();
    await page.keyboard.press("Escape");
    await expect(open).toBeFocused();
});

test("hide, ancestor hide, programmatic remove and missing nested opener restore focus", async ({
    page
}) => {
    await page.goto("/all-widgets");
    const open = button(page, "Open lifecycle modal");
    for (const name of ["Hide lifecycle modal", "Hide modal ancestor"]) {
        await open.click();
        await button(page, name).click();
        await expect(open).toBeFocused();
    }
    await open.click();
    const opener = button(page, "Open nested modal");
    await opener.click();
    await button(page, "Close nested modal").click();
    await expect(opener).toBeFocused();
    await opener.click();
    await button(page, "Remove opener and close").click();
    await expect(opener).toHaveCount(0);
    await expect(button(page, "Hide lifecycle modal")).toBeFocused();
});

test("text-only dialog is the focus stop, then falls back to the page if its opener was removed", async ({
    page
}) => {
    await page.goto("/all-widgets");
    const empty = button(page, "Open text-only dialog");
    const removed = button(page, "Open and remove launcher");
    const dialog = page.getByRole("dialog", { name: "Text-only dialog", exact: true });
    await empty.click();
    await expect(dialog).toBeFocused();
    await page.keyboard.press("Tab");
    await expect(dialog).toBeFocused();
    await page.keyboard.press("Shift+Tab");
    await expect(dialog).toBeFocused();
    await page.keyboard.press("Escape");
    await expect(empty).toBeFocused();
    await removed.click();
    await expect(dialog).toBeFocused();
    await page.keyboard.press("Escape");
    await expect(dialog).toHaveCount(0);
    await expect(page.locator(".widgets-page")).toBeFocused();
    await expect(empty).toBeVisible();
});

test.describe("narrow touch dialog", () => {
    test.use({ viewport: { width: 320, height: 680 }, hasTouch: true });

    test("nested panel keeps its edges visible and tapping backdrop restores the outer focus", async ({
        page
    }) => {
        await page.goto("/all-widgets");
        await button(page, "Open lifecycle modal").tap();
        const opener = button(page, "Open nested modal");
        await opener.tap();
        const inner = page.getByRole("dialog", { name: "Nested lifecycle modal", exact: true });
        await expect(button(inner, "Close nested modal")).toBeFocused();
        const box = await inner.boundingBox();
        expect(box.x).toBeGreaterThanOrEqual(8);
        expect(box.x + box.width).toBeLessThanOrEqual(312);
        await page.touchscreen.tap(5, 5);
        await expect(inner).toHaveCount(0);
        await expect(opener).toBeFocused();
    });
});
