import { test, expect } from "@playwright/test";

test("tree focus, selection, lazy states and navigation", async ({ page }) => {
    await page.goto("/all-widgets");
    const tree = page.getByRole("tree", { name: "Категории украшений" });
    const row = name => tree.getByRole("treeitem", { name, exact: true });
    const active = async name =>
        expect(tree).toHaveAttribute("aria-activedescendant", await row(name).getAttribute("id"));
    await tree.focus();
    await active("Витражное стекло");
    await tree.press("ArrowRight");
    await expect(tree.getByRole("status")).toHaveText("Загрузка…");
    await expect(row("Подвески")).toBeVisible();
    await tree.press("ArrowRight");
    await active("Подвески");
    await tree.press("Space");
    await expect(row("Подвески")).toHaveAttribute("aria-selected", "true");
    await tree.press("ArrowDown");
    await active("Серьги из цветного стекла");
    await tree.press("ArrowUp");
    await tree.press("ArrowLeft");
    await active("Витражное стекло");
    await tree.press("ArrowLeft");
    await expect(row("Подвески")).toHaveCount(0);
    await tree.press("End");
    await active("Недоступный архив");
    await tree.press("ArrowRight");
    await expect(tree.getByText("Ошибка загрузки")).toBeVisible();
    await tree.press("ArrowUp");
    await tree.press("ArrowRight");
    await expect(tree.getByText("Пока пусто")).toBeVisible();
    await tree.press("Home");
    await active("Витражное стекло");
    await page.getByRole("button", { name: "Отключить дерево" }).click();
    await expect(tree).toHaveAttribute("aria-disabled", "true");
});

test("old request cannot overwrite the new branch", async ({ page }) => {
    await page.goto("/tree-race");
    const tree = page.getByRole("tree");
    const root = tree.getByRole("treeitem", { name: "Root" });
    await root.locator("[data-toggle]").click();
    await expect(tree.getByRole("status")).toHaveText("Loading");
    await tree.press("ArrowLeft");
    await tree.press("ArrowRight");
    await page.getByRole("button", { name: "Finish both" }).click();
    await expect(tree.getByRole("treeitem", { name: "Current" })).toBeVisible();
    await expect(tree.getByText("Stale")).toHaveCount(0);
});

test.describe("touch layout", () => {
    test.use({ viewport: { width: 360, height: 740 }, hasTouch: true });
    test("nested Russian labels wrap and remain selectable", async ({ page }) => {
        await page.goto("/all-widgets");
        const tree = page.getByRole("tree");
        for (const name of ["Витражное стекло", "Подвески"]) {
            await tree
                .getByRole("treeitem", { name, exact: true })
                .locator("[data-toggle]")
                .first()
                .tap();
        }
        const leaf = tree.getByRole("treeitem", { name: /^Коллекция длинных/ });
        await leaf.tap();
        await expect(leaf).toHaveAttribute("aria-selected", "true");
        expect(await tree.evaluate(node => node.scrollWidth <= node.clientWidth)).toBe(true);
    });
});
