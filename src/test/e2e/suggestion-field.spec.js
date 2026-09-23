import { test, expect } from "@playwright/test";

test("keyboard selection and saved values travel through Java models", async ({ page }) => {
    await page.goto("/suggestions");
    const first = page.getByRole("combobox").nth(0);
    const next = page.getByRole("combobox").nth(1);
    await first.focus();
    await expect(page.getByRole("option")).toHaveCount(3);
    await first.fill("РОД");
    await expect(page.getByRole("option")).toHaveText(["Латунь с родиевым покрытием"]);
    await first.press("ArrowDown");
    await first.press("Enter");
    await expect(first).toHaveValue("Латунь с родиевым покрытием");
    await expect(
        page.getByText("Value: Латунь с родиевым покрытием", { exact: true })
    ).toBeVisible();
    await expect(page.getByRole("listbox")).toHaveCount(0);
    await first.fill("Медь");
    await next.focus();
    await expect(page.getByRole("option")).toHaveCount(3);
    await page.getByRole("button", { name: "Save value" }).click();
    await next.focus();
    await expect(page.getByRole("option", { name: "Медь", exact: true })).toBeVisible();
    await next.press("Escape");
    await expect(next).toHaveValue("");
    await expect(page.getByRole("listbox")).toHaveCount(0);
});

test.describe("mobile suggestions", () => {
    test.use({ viewport: { width: 390, height: 700 }, hasTouch: true, isMobile: true });

    test("tap selects editable text and an outside tap dismisses", async ({ page }) => {
        await page.goto("/suggestions");
        const first = page.getByRole("combobox").first();
        await first.tap();
        await page.getByRole("option", { name: "Серебро", exact: true }).tap();
        await expect(first).toHaveValue("Серебро");
        await expect(page.getByText("Value: Серебро", { exact: true })).toBeVisible();
        await expect(page.getByRole("listbox")).toHaveCount(0);
        await first.fill("Серебро 925");
        await expect(page.getByText("Value: Серебро 925", { exact: true })).toBeVisible();
        await first.fill("");
        await expect(page.getByRole("option")).toHaveCount(3);
        await page.touchscreen.tap(380, 600);
        await expect(page.getByRole("listbox")).toHaveCount(0);
        expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
            390
        );
    });
});
