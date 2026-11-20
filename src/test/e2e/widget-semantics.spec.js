import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

test("native named icon, headings, informative and decorative images in AllWidgets", async ({
    page
}, testInfo) => {
    await page.goto("/all-widgets");
    await expect(page.getByRole("heading", { name: "All widgets", level: 1 })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Accessible widgets", level: 2 })).toBeVisible();
    await expect(page.getByRole("button", { name: "Add item" })).toBeVisible();
    const action = page.getByRole("button", { name: "Refresh preview", exact: true });
    await action.focus();
    await page.keyboard.press("Tab");
    await expect(page.getByRole("textbox", { name: "Delivery code" })).toBeFocused();
    await page.keyboard.press("Shift+Tab");
    await expect(action).toBeFocused();
    await expect(action).toHaveCSS("outline-style", "solid");
    await page.keyboard.press("Enter");
    await expect(page.getByText("Refresh count: 1")).toBeVisible();
    await expect(page.getByRole("button", { name: "Refresh unavailable" })).toBeDisabled();
    await expect(page.getByRole("img", { name: "North marker" })).toBeVisible();
    const decorative = page
        .getByText("DECORATIVE IMAGE", { exact: true })
        .locator("xpath=ancestor::div[.//img][1]");
    await expect(decorative.locator("img")).toBeVisible();
    await expect(decorative.locator("img")).toHaveAttribute("alt", "");
    await expect(decorative.getByRole("img")).toHaveCount(0);

    const result = await new AxeBuilder({ page }).analyze();
    const findings = result.violations.map(v => ({ id: v.id, nodes: v.nodes.length }));
    console.log("AllWidgets axe:", JSON.stringify(findings));
    await testInfo.attach("AllWidgets axe report", {
        body: JSON.stringify(result.violations, null, 2),
        contentType: "application/json"
    });
    const scoped = await new AxeBuilder({ page })
        .include(
            'button[aria-label="Refresh preview"], button[aria-label="Refresh unavailable"], ' +
                'img[alt="North marker"], input[aria-labelledby][aria-errormessage]'
        )
        .analyze();
    expect(scoped.violations.map(v => v.id)).toEqual([]);
});

test("visible label updates the name and disposal clears ID references", async ({ page }) => {
    await page.goto("/all-widgets");
    const field = page.getByRole("textbox", { name: "Delivery code" });
    await expect(field).toHaveAccessibleDescription("Use four digits. The code is incomplete.");
    await expect(field).toHaveAttribute("aria-invalid", "true");
    const errorId = await field.getAttribute("aria-errormessage");
    expect(errorId).toBeTruthy();
    expect(await page.evaluate(id => document.getElementById(id)?.textContent, errorId)).toBe(
        "The code is incomplete."
    );
    await page.getByRole("button", { name: "Toggle field validity" }).click();
    await expect(field).not.toHaveAttribute("aria-errormessage", /.+/);
    await page.getByRole("button", { name: "Rename field label" }).click();
    await expect(page.getByRole("textbox", { name: "Updated delivery code" })).toBeVisible();
    await page.getByRole("button", { name: "Remove field label" }).click();
    const fallback = page.getByRole("textbox", { name: "Shipping code" });
    await expect(fallback).toBeVisible();
    await expect(fallback).not.toHaveAttribute("aria-labelledby", /.+/);
    await expect(fallback).toHaveAccessibleDescription("Use four digits.");
});

test.describe("icon action with touch", () => {
    test.use({ viewport: { width: 320, height: 680 }, hasTouch: true });

    test("activates once on a narrow viewport", async ({ page }) => {
        await page.goto("/all-widgets");
        const action = page.getByRole("button", { name: "Refresh preview", exact: true });
        await action.tap();
        await expect(page.getByText("Refresh count: 1")).toBeVisible();
    });
});
