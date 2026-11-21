import { expect, test } from "@playwright/test";

const warehouse = page => page.getByRole("combobox", { name: "Warehouse search" });
const status = page => page.locator(".typed-choice-popup:visible").getByRole("status");

test("stable native keys distinguish duplicate captions and allow clearing", async ({ page }) => {
    await page.goto("/all-widgets");
    const single = page.getByRole("combobox", { name: "Material: single" });
    const multi = page.getByRole("listbox", { name: "Materials: multiple" });
    await expect(single).toHaveValue("");
    await expect(single.locator("option", { hasText: "Paper" })).toHaveCount(2);
    await single.selectOption("paper-b");
    await expect(page.getByText("Material code: 102")).toBeVisible();
    await expect(single.locator('option[value="archive"]')).toBeDisabled();
    await page.getByRole("button", { name: "Reorder materials" }).click();
    await expect(single.locator("option").nth(1)).toHaveText("Archive (disabled)");
    await expect(single).toHaveValue("paper-b");
    await page.getByRole("button", { name: "Remove second paper" }).click();
    await expect(single).toHaveValue("");
    await expect(page.getByText("Material code: none")).toBeVisible();
    await expect(multi).toHaveValues(["paper-a", "ink"]);
    await multi.selectOption(["ink"]);
    await expect(multi).toHaveValues(["ink"]);
    await page.getByRole("button", { name: "Clear materials" }).click();
    await expect(multi).toHaveValues([]);
    await expect(page.getByRole("combobox", { name: "Disabled selection" })).toBeDisabled();
});

test("cancelled slow provider cannot replace fast results; focus and keys survive", async ({
    page
}) => {
    await page.goto("/all-widgets");
    const search = warehouse(page);
    await search.fill("slow");
    await expect(status(page)).toHaveText("Loading choices…");
    await search.fill("fast");
    await expect(page.getByRole("option", { name: "Fast result" })).toBeVisible();
    await expect(search).toBeFocused();
    await expect(search).toHaveValue("fast");
    await search.press("ArrowDown");
    await expect(search).toHaveAttribute("aria-activedescendant", /typed-combo/);
    await search.press("Enter");
    await expect(search).toHaveValue("Fast result");
    await expect(page.getByText("Warehouse code: 202")).toBeVisible();
    await expect(search).toBeFocused();
    await search.fill("error");
    await expect(status(page)).toHaveText("Choices unavailable. Edit the search to retry.");
    await search.press("Escape");
    await expect(search).toHaveValue("Fast result");
    await expect(search).toHaveAttribute("aria-expanded", "false");
    await search.fill("none");
    await expect(status(page)).toHaveText("No choices");
    await search.press("Tab");
    await expect(search).toHaveValue("Fast result");
    await expect(search).toHaveAttribute("aria-expanded", "false");
});

test("IME input waits for composition; free text stays separate from suggestions", async ({
    page
}) => {
    await page.goto("/all-widgets");
    const search = warehouse(page);
    await search.focus();
    await search.evaluate(input => {
        input.dispatchEvent(new CompositionEvent("compositionstart"));
        input.value = "fast";
        input.dispatchEvent(new InputEvent("input", { bubbles: true, isComposing: true }));
    });
    await search.press("Enter");
    await expect(search).toHaveAttribute("aria-expanded", "false");
    await search.evaluate(input =>
        input.dispatchEvent(new CompositionEvent("compositionend", { data: "fast" }))
    );
    await expect(page.getByRole("option", { name: "Fast result" })).toBeVisible();
    await expect(search).toBeFocused();
    await search.press("Escape");

    const free = page.getByRole("combobox", { name: "Note or material" });
    await expect(free).toHaveAttribute("aria-invalid", "true");
    await free.fill("Free text");
    await expect(status(page)).toHaveText("No choices");
    await free.press("Escape");
    await expect(free).toHaveValue("Free text");
    await free.locator("..").getByRole("button", { name: "Clear choice" }).click();
    await expect(free).toHaveValue("");
    await expect(free).toBeFocused();
});

test.describe("390 px touchscreen choice popup", () => {
    test.use({ viewport: { width: 390, height: 800 }, hasTouch: true });
    test("pagination, long labels, and normal tap selection keep the input", async ({ page }) => {
        await page.goto("/all-widgets");
        const search = warehouse(page);
        await search.tap();
        const long = page.getByRole("option", { name: /^VeryLongWarehouseLabel/ });
        await expect(long).toBeVisible();
        expect(await long.evaluate(option => option.scrollWidth <= option.clientWidth + 1)).toBe(
            true
        );
        await expect(page.getByRole("option", { name: "Closed warehouse" })).toHaveAttribute(
            "aria-disabled",
            "true"
        );
        await page.getByRole("button", { name: "Load more" }).tap();
        const last = page.getByRole("option", { name: "Backup warehouse 20" });
        await expect(last).toBeAttached();
        await last.tap();
        await expect(search).toHaveValue("Backup warehouse 20");
        await expect(page.getByText("Warehouse code: 320")).toBeVisible();
        await expect(search).toHaveAttribute("aria-expanded", "false");
    });
});
