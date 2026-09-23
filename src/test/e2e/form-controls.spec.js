import { test, expect } from "@playwright/test";

test.use({ viewport: { width: 390, height: 844 }, hasTouch: true, isMobile: true });

test("standard controls have readable font sizes and preserve touch activation", async ({
    page
}) => {
    await page.goto("/form-controls");
    const controls = page.locator("input, textarea, select");
    await expect(controls).toHaveCount(6);
    for (let index = 0; index < 6; index++) {
        const control = controls.nth(index);
        const size = index === 5 ? "20px" : "16px";
        await expect(control).toHaveCSS("font-size", size);
        await expect(control).toHaveCSS("touch-action", "manipulation");
        await control.focus();
        await expect(control).toBeFocused();
        await expect(control).toHaveCSS("font-size", size);
    }
    const button = page.getByRole("button", { name: "Tap twice" });
    await expect(button).toHaveCSS("touch-action", "manipulation");
    await expect(page.getByRole("button", { name: "Choose file" })).toHaveCSS(
        "touch-action",
        "manipulation"
    );
    await button.tap();
    await button.tap();
    await expect(page.getByText("Clicks: 2", { exact: true })).toBeVisible();
    await expect(page.locator('meta[name="viewport"]')).toHaveAttribute(
        "content",
        "width=device-width, initial-scale=1"
    );
});
