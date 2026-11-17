import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

test.afterEach(async ({ page }) => {
    await expect(page.locator("#client-error-overlay")).toHaveCount(0);
    await expect(page.locator("#connection-terminated-overlay")).toHaveCount(0);
});

async function insideViewport(page, overlay) {
    const result = await overlay.evaluate(el => {
        const box = el.getBoundingClientRect(),
            view = window.visualViewport;
        return {
            left: box.left,
            top: box.top,
            right: box.right,
            bottom: box.bottom,
            view: {
                left: view?.offsetLeft || 0,
                top: view?.offsetTop || 0,
                width: view?.width || innerWidth,
                height: view?.height || innerHeight
            }
        };
    });
    expect(result.left).toBeGreaterThanOrEqual(result.view.left + 7);
    expect(result.top).toBeGreaterThanOrEqual(result.view.top + 7);
    expect(result.right).toBeLessThanOrEqual(result.view.left + result.view.width - 7);
    expect(result.bottom).toBeLessThanOrEqual(result.view.top + result.view.height - 7);
}

for (const width of [320, 1280]) {
    test.describe(`anchored overlays ${width}px`, () => {
        test.use({ hasTouch: true });
        test("menu keyboard, disabled actions, once-only choice and long touch menu", async ({
            page
        }) => {
            await page.setViewportSize({ width, height: 850 });
            await page.goto("/all-widgets");
            const trigger = page.getByRole("button", { name: "Действия с изделием", exact: true });
            await trigger.scrollIntoViewIfNeeded();
            await trigger.press("ArrowDown");
            const menu = page.getByRole("menu", { name: "Действия с изделием", exact: true });
            await expect(menu).toBeVisible();
            await insideViewport(page, menu);
            const edit = menu.getByRole("menuitem", { name: "Редактировать описание изделия" });
            const copy = menu.getByRole("menuitem", { name: "Создать копию" });
            await expect(edit).toBeFocused();
            await edit.press("ArrowDown");
            await expect(copy).toBeFocused();
            await copy.press("Home");
            await expect(edit).toBeFocused();
            await edit.press("End");
            await expect(copy).toBeFocused();
            await copy.press("Enter");
            await expect(menu).toBeHidden();
            await expect(
                page.getByText("Копирование · Действий: 1", { exact: true })
            ).toBeVisible();
            await expect(trigger).toBeFocused();
            await trigger.press("ArrowUp");
            await expect(copy).toBeFocused();
            await copy.press("Escape");
            await expect(trigger).toBeFocused();
            await expect(menu).toBeHidden();
            const longTrigger = page.getByRole("button", { name: "Длинное меню", exact: true });
            if (width === 320) await longTrigger.tap();
            else await longTrigger.click();
            const longMenu = page.getByRole("menu", { name: "Длинное меню действий" });
            await expect(longMenu).toBeVisible();
            await insideViewport(page, longMenu);
            await longMenu.getByRole("menuitem").first().press("End");
            const last = longMenu.getByRole("menuitem").last();
            await expect(last).toBeFocused();
            if (width === 320) await last.tap();
            else await last.click();
            await expect(page.getByText("Выбран пункт 16", { exact: true })).toBeVisible();
            await expect(longMenu).toBeHidden();
        });

        test("popover keeps edits, resizes around keyboard, closes with outside click and removed anchor", async ({
            page
        }) => {
            await page.setViewportSize({ width, height: 850 });
            await page.goto("/all-widgets");
            const trigger = page.getByRole("button", { name: "Настройки доставки", exact: true });
            await trigger.click();
            const popover = page.getByRole("dialog", { name: "Параметры доставки", exact: true });
            await expect(popover).toBeVisible();
            const field = popover.getByRole("textbox", { name: "Комментарий к доставке" });
            await expect(field).toBeFocused();
            await field.fill("Сохранённая заметка");
            await popover.getByRole("button", { name: "Подробнее", exact: true }).click();
            await expect(popover.getByText(/Длинное пояснение/)).toBeVisible();
            await insideViewport(page, popover);
            await page.setViewportSize({ width, height: 420 });
            await expect
                .poll(async () => popover.evaluate(el => el.getBoundingClientRect().bottom))
                .toBeLessThanOrEqual(413);
            await insideViewport(page, popover);
            await field.press("Escape");
            await expect(popover).toBeHidden();
            await expect(trigger).toBeFocused();
            await page.setViewportSize({ width, height: 850 });
            await trigger.click();
            await expect(field).toHaveValue("Сохранённая заметка");
            await page
                .getByRole("heading", { name: "Меню и подсказки у элемента", exact: true })
                .click();
            await expect(popover).toBeHidden();
            await trigger.click();
            await expect(popover).toBeVisible();
            // Programmatic click preserves popover focus while the server removes its anchor.
            await page
                .getByRole("button", { name: "Убрать настройки", exact: true })
                .evaluate(el => el.click());
            await expect(trigger).toHaveCount(0);
            await expect(popover).toBeHidden();
        });
    });
}

test("tooltip delays cancel, accessible descriptions coexist and Escape never moves focus", async ({
    page
}) => {
    await page.goto("/all-widgets");
    const help = page.getByRole("button", { name: "Срок изготовления", exact: true });
    await expect(help).toHaveAccessibleDescription(/Срок зависит от наличия материалов/);
    await help.hover();
    const tooltip = page.getByRole("tooltip");
    await expect(tooltip).toBeVisible();
    await page.getByRole("heading", { name: "Меню и подсказки у элемента", exact: true }).hover();
    await expect(tooltip).toBeHidden();
    await help.focus();
    await expect(tooltip).toBeVisible();
    await help.press("Escape");
    await expect(tooltip).toBeHidden();
    await expect(help).toBeFocused();
    await expect(help).toHaveAccessibleDescription(/Точную дату согласуем/);
    await help.blur();
    await help.hover();
    await page.mouse.move(0, 0);
    await expect(tooltip).toBeHidden();
});

test("nested menu consumes Escape before Dialog and overlays pass axe", async ({ page }) => {
    await page.goto("/all-widgets");
    await page.getByRole("button", { name: "Меню в диалоге", exact: true }).click();
    const dialog = page.getByRole("dialog", { name: "Диалог со своим меню", exact: true });
    await dialog.getByRole("button", { name: "Вложенные действия", exact: true }).click();
    const menu = page.getByRole("menu", { name: "Действия внутри диалога", exact: true });
    await expect(menu).toBeVisible();
    await menu.getByRole("menuitem").press("Escape");
    await expect(menu).toBeHidden();
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole("button", { name: "Вложенные действия" })).toBeFocused();
    await dialog.getByRole("button", { name: "Вложенные действия" }).press("Escape");
    await expect(dialog).toBeHidden();
    await page.getByRole("button", { name: "Действия с изделием", exact: true }).click();
    expect(
        (await new AxeBuilder({ page }).include(".widgets-anchored-menu:popover-open").analyze())
            .violations
    ).toEqual([]);
    await page.getByRole("menuitem", { name: "Создать копию" }).press("Escape");
    await page.getByRole("button", { name: "Настройки доставки", exact: true }).click();
    expect(
        (await new AxeBuilder({ page }).include(".widgets-anchored-popover:popover-open").analyze())
            .violations
    ).toEqual([]);
});

test("top-layer placement survives transformed scrolling ancestors and flips near viewport edges", async ({
    page
}) => {
    await page.setViewportSize({ width: 800, height: 600 });
    await page.goto("/all-widgets");
    const trigger = page.getByRole("button", { name: "Действия с изделием", exact: true });
    await trigger.scrollIntoViewIfNeeded();
    // A real overflow/transform ancestor exercises native top-layer geometry, not mocked rectangles.
    await trigger.evaluate(anchor => {
        const box = document.createElement("div");
        box.id = "overlay-scroll-fixture";
        box.style.cssText =
            "position:fixed;left:20px;top:440px;width:400px;height:130px;overflow:auto;transform:translateZ(0);z-index:100";
        anchor.before(box);
        const spacer = document.createElement("div");
        spacer.style.height = "30px";
        box.append(spacer, anchor);
        const tail = document.createElement("div");
        tail.style.height = "400px";
        box.append(tail);
    });
    await trigger.click();
    const menu = page.getByRole("menu", { name: "Действия с изделием", exact: true });
    await expect(menu).toBeVisible();
    await expect(menu).toHaveAttribute("data-placement", "top");
    await insideViewport(page, menu);
    const before = await menu.boundingBox();
    await page.locator("#overlay-scroll-fixture").evaluate(el => {
        el.scrollTop = 20;
    });
    await expect.poll(async () => (await menu.boundingBox()).y).toBeLessThan(before.y - 10);
    await expect(menu).toBeVisible();
    await page.locator("#overlay-scroll-fixture").evaluate(el => {
        el.scrollTop = 160;
    });
    await expect(menu).toBeHidden();
});

test("hiding an ancestor accepts cleanup and reopening remains interactive", async ({ page }) => {
    await page.goto("/anchored-lifecycle");
    const anchor = page.getByRole("button", { name: "Lifecycle actions", exact: true });
    await anchor.click();
    const menu = page.locator(".widgets-anchored-menu");
    await expect(menu).toBeVisible();
    // Hide via the server without triggering the outside-pointer dismissal first.
    await page
        .getByRole("button", { name: "Hide container", exact: true })
        .evaluate(el => el.click());
    await expect(menu).toBeHidden();
    await expect(menu.locator(":scope > div")).toHaveCSS("display", "none");
    await page.getByRole("button", { name: "Show container", exact: true }).click();
    await anchor.click();
    await expect(menu).toBeVisible();
    await menu.getByRole("menuitem", { name: "Run action", exact: true }).press("Enter");
    await expect(page.getByText("Action completed", { exact: true })).toBeVisible();
    await expect(menu).toBeHidden();
});
