import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

test.use({ hasTouch: true });

for (const width of [1280, 390]) {
    test(`feedback keeps focus and supports retry, cancellation and dismissal at ${width}px`, async ({
        page
    }) => {
        await page.setViewportSize({ width, height: 900 });
        await page.goto("/all-widgets");
        await expect(page.locator(".widgets-feedback").first()).toBeVisible();
        const accessibility = await new AxeBuilder({ page }).include(".widgets-feedback").analyze();
        expect(accessibility.violations).toEqual([]);
        const cancel = page.getByRole("button", { name: "Отменить загрузку", exact: true });
        await expect(cancel).toBeVisible();
        const region = cancel.locator("xpath=ancestor::*[contains(@class,'widgets-feedback')][1]");
        await expect(region.getByRole("progressbar")).toHaveAttribute("value", "40");
        if (width === 390) await cancel.tap();
        else await cancel.click();
        await expect(region.getByText("Загрузка отменена", { exact: true })).toBeVisible();
        await expect(cancel).toBeDisabled();
        await expect(region.getByRole("progressbar")).toHaveCount(0);
        await page.getByRole("button", { name: "Показать пустой результат", exact: true }).click();
        await expect(
            page.getByText("Проверка завершена: заказов нет", { exact: true })
        ).toBeVisible();
        await page.getByRole("button", { name: "Начать проверку", exact: true }).click();
        await expect(
            page.getByRole("progressbar", { name: "Проверяем наличие изделий…" })
        ).toBeVisible();
        await page.getByRole("button", { name: "Ошибка проверки", exact: true }).click();
        await expect(
            page.getByText("Проверка не удалась. Попробуйте ещё раз.", { exact: true })
        ).toBeVisible();
        const retry = page.getByRole("button", { name: "Загрузить ещё раз", exact: true });
        await retry.click();
        await expect(page.getByText("Найдено изделий: 2", { exact: true })).toBeVisible();
        await expect(page.getByText("Проверка завершена", { exact: true })).toBeVisible();
        const notify = page.getByRole("button", { name: "Показать уведомление", exact: true });
        await notify.focus();
        await notify.press("Enter");
        await expect(page.getByText("Изделие добавлено в список.", { exact: true })).toBeVisible();
        await expect(notify).toBeFocused();
        await expect(page.getByText("Изделие добавлено в список.", { exact: true })).toHaveCount(
            0,
            { timeout: 9000 }
        );
        const error = page.getByText("Не удалось связаться с сервером. Повторите попытку.", {
            exact: true
        });
        await expect(error).toBeVisible();
        await error.locator("..").getByRole("button", { name: "Закрыть уведомление" }).click();
        await expect(error).toHaveCount(0);
        const overflows = await page
            .locator(".widgets-feedback")
            .evaluateAll(nodes =>
                nodes
                    .filter(node => node.scrollWidth > node.clientWidth + 1)
                    .map(node => node.textContent)
            );
        expect(overflows).toEqual([]);
    });
}
