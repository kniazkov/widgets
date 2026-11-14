import { expect, test } from "@playwright/test";

const warehouse = page => page.getByRole("combobox", { name: "Поиск склада" });
const status = page => page.locator(".typed-choice-popup:visible").getByRole("status");

test("stable native keys distinguish duplicate captions and allow clearing", async ({ page }) => {
    await page.goto("/all-widgets");
    const single = page.getByRole("combobox", { name: "Материал: один" });
    const multi = page.getByRole("listbox", { name: "Материалы: несколько" });
    await expect(single).toHaveValue("");
    await expect(single.locator("option", { hasText: "Бумага" })).toHaveCount(2);
    await single.selectOption("paper-b");
    await expect(page.getByText("Код материала: 102")).toBeVisible();
    await expect(single.locator('option[value="archive"]')).toBeDisabled();
    await page.getByRole("button", { name: "Переставить материалы" }).click();
    await expect(single.locator("option").nth(1)).toHaveText("Архив (недоступно)");
    await expect(single).toHaveValue("paper-b");
    await page.getByRole("button", { name: "Убрать вторую бумагу" }).click();
    await expect(single).toHaveValue("");
    await expect(page.getByText("Код материала: нет")).toBeVisible();
    await expect(multi).toHaveValues(["paper-a", "ink"]);
    await multi.selectOption(["ink"]);
    await expect(multi).toHaveValues(["ink"]);
    await page.getByRole("button", { name: "Очистить материалы" }).click();
    await expect(multi).toHaveValues([]);
    await expect(page.getByRole("combobox", { name: "Недоступный выбор" })).toBeDisabled();
});

test("cancelled slow provider cannot replace fast results; focus and keys survive", async ({
    page
}) => {
    await page.goto("/all-widgets");
    const search = warehouse(page);
    await search.fill("медленно");
    await expect(status(page)).toHaveText("Loading choices…");
    await search.fill("быстро");
    await expect(page.getByRole("option", { name: "Быстрый результат" })).toBeVisible();
    await expect(search).toBeFocused();
    await expect(search).toHaveValue("быстро");
    await search.press("ArrowDown");
    await expect(search).toHaveAttribute("aria-activedescendant", /typed-combo/);
    await search.press("Enter");
    await expect(search).toHaveValue("Быстрый результат");
    await expect(page.getByText("Код склада: 202")).toBeVisible();
    await expect(search).toBeFocused();
    await search.fill("ошибка");
    await expect(status(page)).toHaveText("Choices unavailable. Edit the search to retry.");
    await search.press("Escape");
    await expect(search).toHaveValue("Быстрый результат");
    await expect(search).toHaveAttribute("aria-expanded", "false");
    await search.fill("нет");
    await expect(status(page)).toHaveText("No choices");
    await search.press("Tab");
    await expect(search).toHaveValue("Быстрый результат");
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
        input.value = "быстро";
        input.dispatchEvent(new InputEvent("input", { bubbles: true, isComposing: true }));
    });
    await search.press("Enter");
    await expect(search).toHaveAttribute("aria-expanded", "false");
    await search.evaluate(input =>
        input.dispatchEvent(new CompositionEvent("compositionend", { data: "быстро" }))
    );
    await expect(page.getByRole("option", { name: "Быстрый результат" })).toBeVisible();
    await expect(search).toBeFocused();
    await search.press("Escape");

    const free = page.getByRole("combobox", { name: "Примечание или материал" });
    await expect(free).toHaveAttribute("aria-invalid", "true");
    await free.fill("Свободный текст");
    await expect(status(page)).toHaveText("No choices");
    await free.press("Escape");
    await expect(free).toHaveValue("Свободный текст");
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
        const long = page.getByRole("option", { name: /^ОченьДлиннаяПодпись/ });
        await expect(long).toBeVisible();
        expect(await long.evaluate(option => option.scrollWidth <= option.clientWidth + 1)).toBe(
            true
        );
        await expect(page.getByRole("option", { name: "Закрытый склад" })).toHaveAttribute(
            "aria-disabled",
            "true"
        );
        await page.getByRole("button", { name: "Load more" }).tap();
        const last = page.getByRole("option", { name: "Резервный склад 20" });
        await expect(last).toBeAttached();
        await last.tap();
        await expect(search).toHaveValue("Резервный склад 20");
        await expect(page.getByText("Код склада: 320")).toBeVisible();
        await expect(search).toHaveAttribute("aria-expanded", "false");
    });
});
