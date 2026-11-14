import { expect, test } from "@playwright/test";

function field(page, name) {
    return page.getByRole("textbox", { name });
}

function form(page) {
    return page.getByRole("form", { name: "Адаптивная учебная заявка" });
}

const title = "Полное описание учебного предмета для проверки переноса подписи";
const longWord = "ДлинноесловобезпробеловДляПроверкиПереносаСетки";

async function placement(first, second) {
    const a = await first.boundingBox();
    const b = await second.boundingBox();
    return { dx: Math.round(b.x - a.x), dy: Math.round(b.y - a.y) };
}

test("stack direction and nested grids follow their own width, not the device", async ({
    page
}) => {
    await page.setViewportSize({ width: 1280, height: 780 });
    await page.goto("/all-widgets");
    const alpha = page.getByText("Альфа · Stack", { exact: true });
    const beta = page.getByText("Бета · Stack", { exact: true });
    const wideA = page.getByText("Широкая сетка А", { exact: true });
    const wideB = page.getByText("Широкая сетка Б", { exact: true });
    const innerA = page.getByText("Узкая сетка А", { exact: true });
    const innerB = page.getByText("Узкая сетка Б", { exact: true });
    await expect(beta).toBeVisible();
    expect(await placement(alpha, beta)).toMatchObject({
        dx: expect.any(Number),
        dy: 0
    });
    expect((await placement(alpha, beta)).dx).toBeGreaterThan(0);
    expect((await placement(wideA, wideB)).dx).toBeGreaterThan(0);
    expect((await placement(innerA, innerB)).dy).toBeGreaterThan(0);
    await page.getByRole("button", { name: "Сменить направление Stack" }).click();
    await expect.poll(async () => (await placement(alpha, beta)).dy).toBeGreaterThan(0);
    await page.setViewportSize({ width: 320, height: 780 });
    await expect.poll(async () => (await placement(wideA, wideB)).dy).toBeGreaterThan(0);
    expect((await placement(innerA, innerB)).dy).toBeGreaterThan(0);
    expect(
        await page.getByText(longWord, { exact: true }).evaluate(node => {
            const range = document.createRange();
            range.selectNodeContents(node);
            const grid = node.closest(".widgets-responsive-grid");
            return range.getClientRects().length > 1 && grid.scrollWidth <= grid.clientWidth + 1;
        })
    ).toBe(true);
});

test("form columns become rows on resize and retain input DOM identity, selection and focus", async ({
    page
}) => {
    await page.setViewportSize({ width: 1280, height: 780 });
    await page.goto("/all-widgets");
    const group = form(page);
    const layout = group.locator(".widgets-form-layout");
    const editor = field(page, title);
    const label = group.locator("label").filter({ hasText: title });
    await expect(layout).toHaveAttribute("data-label-columns", "");
    expect((await placement(label, editor)).dx).toBeGreaterThan(100);
    await label.click();
    await expect(editor).toBeFocused();
    await editor.fill("Бумага");
    await editor.evaluate(node => {
        node.setSelectionRange(2, 4);
        node.dataset.resizeProbe = "original";
    });
    await page.setViewportSize({ width: 390, height: 780 });
    await expect(layout).not.toHaveAttribute("data-label-columns", "");
    expect((await placement(label, editor)).dy).toBeGreaterThan(10);
    await expect(editor).toBeFocused();
    await expect(editor).toHaveValue("Бумага");
    await expect(editor).toHaveAttribute("data-resize-probe", "original");
    expect(await editor.evaluate(node => [node.selectionStart, node.selectionEnd])).toEqual([2, 4]);
    await page.setViewportSize({ width: 1280, height: 780 });
    await expect(layout).toHaveAttribute("data-label-columns", "");
    await expect(editor).toBeFocused();
    await page.getByRole("button", { name: "Сменить порог формы" }).click();
    await expect(layout).not.toHaveAttribute("data-label-columns", "");
    await expect(editor).toHaveAttribute("data-resize-probe", "original");
    await expect(editor).toHaveValue("Бумага");
});

test("200% layout zoom stacks labels and keeps normal Tab / Enter validation", async ({ page }) => {
    await page.setViewportSize({ width: 1024, height: 780 });
    await page.goto("/all-widgets");
    const group = form(page);
    const layout = group.locator(".widgets-form-layout");
    const editor = field(page, title);
    const note = field(page, "Дополнительный комментарий");
    await expect(layout).toHaveAttribute("data-label-columns", "");
    await page.locator("body").evaluate(node => (node.style.zoom = "2"));
    await expect(layout).not.toHaveAttribute("data-label-columns", "");
    await editor.focus();
    await editor.press("Tab");
    await expect(note).toBeFocused();
    await note.press("Enter");
    await expect(group.getByRole("status")).toHaveText("Заполните выделенное поле.");
    await expect(editor).toBeFocused();
    await expect(editor).toHaveAccessibleDescription(/Заполните описание/);
});

test.describe("adaptive controls on a touch viewport", () => {
    test.use({ viewport: { width: 390, height: 780 }, hasTouch: true });
    test("taps a stacked form label and existing submit control", async ({ page }) => {
        await page.goto("/all-widgets");
        const group = form(page);
        const editor = field(page, title);
        await expect(group.locator(".widgets-form-layout")).not.toHaveAttribute(
            "data-label-columns",
            ""
        );
        await group.locator("label").filter({ hasText: title }).tap();
        await expect(editor).toBeFocused();
        await editor.fill("Бумага");
        await group.getByRole("button", { name: "Проверить макет" }).tap();
        await expect(group.getByRole("status")).toHaveText("Макет проверен.");
    });
});
