import { expect, test } from "@playwright/test";

function controls(page) {
    const form = page.getByRole("form", { name: "Заявка с учебными данными" });
    return {
        form,
        title: form.getByRole("textbox", {
            name: "Полное название учебного предмета для демонстрационной заявки"
        }),
        confirm: form.getByRole("textbox", {
            name: "Повторите то же название, чтобы проверить связь между двумя полями"
        }),
        reference: form.getByRole("textbox", { name: "Номер учебного образца" }),
        submit: form.getByRole("button", { name: "Отправить заявку" }),
        status: form.getByRole("status")
    };
}

for (const width of [320, 390, 1280]) {
    test(`Russian form keeps labels, help, required and first-error focus at ${width}px`, async ({
        page
    }) => {
        await page.setViewportSize({ width, height: 760 });
        await page.goto("/all-widgets");
        const { title, confirm, reference, submit, status } = controls(page);
        await expect(title).toHaveAccessibleDescription(/Не более 40 символов/);
        await expect(title).toHaveAttribute("maxlength", "40");
        await expect(title).toHaveAttribute("autocomplete", "off");
        await expect(title).toHaveAttribute("name", "title");
        await expect(title).toHaveAttribute("required", "");
        await expect(reference).toHaveAttribute("readonly", "");
        await expect(reference).toHaveValue("ДЕМО-20");
        await page
            .locator("label")
            .filter({ hasText: "Полное название учебного предмета для демонстрационной заявки" })
            .click();
        await expect(title).toBeFocused();
        await title.press("Tab");
        await expect(confirm).toBeFocused();
        await confirm.press("Enter");
        await expect(status).toHaveText("Проверьте отмеченные поля.");
        await expect(title).toBeFocused();
        await expect(title).toHaveAttribute("aria-invalid", "true");
        await expect(title).toHaveAccessibleDescription(/Не более 40 символов.*Заполните это поле/);
        expect(
            await title.evaluate(el => el.closest(".widgets-form").scrollWidth)
        ).toBeLessThanOrEqual(
            (await title.locator("xpath=ancestor::*[@role='form']").boundingBox()).width + 1
        );
        await title.fill("Бумага");
        await confirm.fill("Синий");
        await submit.click();
        await expect(confirm).toBeFocused();
        await expect(confirm).toHaveAccessibleDescription(
            "Названия в двух полях должны совпадать."
        );
    });
}

test("IME confirmation does not submit; async refusal retains entered text and permits retry", async ({
    page
}) => {
    await page.setViewportSize({ width: 390, height: 760 });
    await page.goto("/all-widgets");
    const { form, title, confirm, submit, status } = controls(page);
    const attempts = page.getByText(/^Учебных отправок: /);
    await title.fill("Занято");
    await confirm.fill("Занято");
    await confirm.evaluate(el => {
        el.dispatchEvent(new CompositionEvent("compositionstart"));
        el.dispatchEvent(
            new KeyboardEvent("keydown", { key: "Enter", isComposing: true, bubbles: true })
        );
        el.dispatchEvent(new CompositionEvent("compositionend"));
        el.dispatchEvent(new KeyboardEvent("keyup", { key: "Enter", bubbles: true }));
    });
    await expect(attempts).toHaveText("Учебных отправок: 0");
    await confirm.press("Enter");
    await expect(form).toHaveAttribute("aria-busy", "true");
    await expect(submit).toBeDisabled();
    await expect(status).toHaveText("Проверяем заявку…");
    // A second deliberate click and Enter are not a transport retry of the first action.
    await submit.scrollIntoViewIfNeeded();
    const box = await submit.boundingBox();
    await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
    await confirm.press("Enter");
    await expect(attempts).toHaveText("Учебных отправок: 1");
    await expect(status).toHaveText("Сервер отклонил учебную заявку.", { timeout: 10000 });
    await expect(title).toHaveValue("Занято");
    await expect(confirm).toHaveValue("Занято");
    await expect(title).toBeFocused();
    await expect(title).toHaveAccessibleDescription(/Этот образец уже зарезервирован/);
    await expect(submit).toBeEnabled();
    await title.fill("Бумага");
    await confirm.fill("Бумага");
    await submit.click();
    await expect(status).toHaveText("Учебная заявка принята.", { timeout: 10000 });
    await expect(attempts).toHaveText("Учебных отправок: 2");
    await expect(title).not.toHaveAttribute("aria-invalid", "true");
});

test.describe("touch form controls", () => {
    test.use({ viewport: { width: 390, height: 760 }, hasTouch: true });
    test("tap the native label and submit button", async ({ page }) => {
        await page.goto("/all-widgets");
        const { title, confirm, submit, status } = controls(page);
        await page
            .locator("label")
            .filter({ hasText: "Полное название учебного предмета для демонстрационной заявки" })
            .tap();
        await expect(title).toBeFocused();
        await title.fill("Бумага");
        await confirm.fill("Бумага");
        await submit.tap();
        await expect(status).toHaveText("Учебная заявка принята.", { timeout: 10000 });
    });
});
