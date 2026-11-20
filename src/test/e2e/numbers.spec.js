import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

for (const width of [390, 1280]) {
    test.describe(`exact numbers at ${width}px`, () => {
        test.use({ hasTouch: true });
        test("decimal drafts, IME, caret, exact submit and live quantity limits", async ({
            page
        }) => {
            await page.setViewportSize({ width, height: 850 });
            await page.goto("/all-widgets");
            const form = page.getByRole("form", { name: "Точные числа", exact: true });
            const amount = form.getByLabel("Точная сумма", { exact: true });
            const count = form.getByLabel("Количество", { exact: true });
            const plus = form.getByRole("button", { name: "Увеличить количество", exact: true });
            const minus = form.getByRole("button", { name: "Уменьшить количество", exact: true });
            const submit = form.getByRole("button", { name: "Проверить числа", exact: true });
            await expect(amount).toHaveAttribute("type", "text");
            await expect(amount).toHaveAttribute("inputmode", "decimal");
            await expect(amount).toHaveValue("9007199254740993,01");
            await submit.click();
            await expect(
                page.getByText("Точно: 9007199254740993.01 / 2", { exact: true })
            ).toBeVisible();
            for (const draft of ["-", "-,", "12,"]) {
                await amount.fill(draft);
                await amount.press("Enter");
                await expect(amount).toHaveValue(draft);
                await expect(amount).toHaveAccessibleDescription(/Введите число с запятой/);
            }
            await amount.fill("12,50");
            await amount.evaluate(el => {
                el.setSelectionRange(1, 1);
                el.dispatchEvent(new CompositionEvent("compositionstart", { bubbles: true }));
            });
            await amount.press("Enter");
            await expect(page.getByText("Точно: 12.50 / 2", { exact: true })).toHaveCount(0);
            await amount.evaluate(el => {
                el.dispatchEvent(new CompositionEvent("compositionend", { bubbles: true }));
                el.dispatchEvent(
                    new InputEvent("input", { bubbles: true, inputType: "insertFromPaste" })
                );
            });
            // Wait for an independent round trip while the editor retains focus and selection.
            await page.waitForFunction(() => {
                const el = document.querySelector('input[name="amount"]');
                return el && !el._textInputPending;
            });
            expect(await amount.evaluate(el => el.selectionStart)).toBe(1);
            await expect(amount).toHaveValue("12,50");
            await amount.press("Enter");
            await expect(page.getByText("Точно: 12.50 / 2", { exact: true })).toBeVisible();
            await amount.fill("12,501");
            await submit.click();
            await expect(amount).toHaveAccessibleDescription(/Проверьте точность и шаг/);
            await amount.fill("9007199254740993,01");
            await count.fill("0");
            await submit.click();
            await expect(minus).toBeDisabled();
            await plus.focus();
            await plus.press("Enter");
            await expect(count).toHaveValue("1");
            if (width === 390) await plus.tap();
            else await plus.click();
            await expect(count).toHaveValue("2");
            await page
                .getByRole("button", { name: "Лимит количества: 2 / 5", exact: true })
                .click();
            await expect(plus).toBeDisabled();
            await expect(count).toHaveValue("2");
            await count.fill("3");
            await submit.click();
            await expect(count).toHaveAccessibleDescription(/вне текущих границ/);
            await expect(plus).toBeDisabled();
            await expect(minus).toBeDisabled();
            await page
                .getByRole("button", { name: "Лимит количества: 2 / 5", exact: true })
                .click();
            await expect(plus).toBeEnabled();
            await expect(count).toHaveValue("3");
            await plus.focus();
            await page.keyboard.down("Space");
            await page.waitForTimeout(650);
            await expect(count).toHaveValue("3");
            await page.keyboard.up("Space");
            await expect(count).toHaveValue("4");
            await plus.click();
            await expect(count).toHaveValue("5");
            await expect(plus).toBeDisabled();
            await expect(page.getByLabel("Недоступное количество", { exact: true })).toBeDisabled();
            await expect(
                page.getByRole("button", { name: "Увеличить недоступное", exact: true })
            ).toBeDisabled();
            await expect(
                page.getByLabel("Число en-US — только чтение", { exact: true })
            ).toHaveValue("1234.50");
            await expect(
                page.getByLabel("Число en-US — только чтение", { exact: true })
            ).toHaveAttribute("readonly", "");
            const bounds = await form.evaluate(el => ({
                width: el.clientWidth,
                scroll: el.scrollWidth
            }));
            expect(bounds.scroll).toBeLessThanOrEqual(bounds.width + 1);
            const audit = await new AxeBuilder({ page }).include(".widgets-number-field").analyze();
            expect(audit.violations).toEqual([]);
        });
    });
}
