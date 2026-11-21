import { test, expect } from "@playwright/test";

test("tree focus, selection, lazy states and navigation", async ({ page }) => {
    await page.goto("/all-widgets");
    const tree = page.getByRole("tree", { name: "Item categories" });
    const row = name => tree.getByRole("treeitem", { name, exact: true });
    const active = async name =>
        expect(tree).toHaveAttribute("aria-activedescendant", await row(name).getAttribute("id"));
    await tree.focus();
    await active("Stained glass");
    await tree.press("ArrowRight");
    await expect(tree.getByRole("status")).toHaveText("Loading…");
    await expect(row("Pendants")).toBeVisible();
    await tree.press("ArrowRight");
    await active("Pendants");
    await tree.press("Space");
    await expect(row("Pendants")).toHaveAttribute("aria-selected", "true");
    await tree.press("ArrowDown");
    await active("Colored glass earrings");
    await tree.press("ArrowUp");
    await tree.press("ArrowLeft");
    await active("Stained glass");
    await tree.press("ArrowLeft");
    await expect(row("Pendants")).toHaveCount(0);
    await tree.press("End");
    await active("Disabled archive");
    await tree.press("ArrowRight");
    await expect(tree.getByText("Loading failed")).toBeVisible();
    await tree.press("ArrowUp");
    await tree.press("ArrowRight");
    await expect(tree.getByText("Nothing here yet")).toBeVisible();
    await tree.press("Home");
    await active("Stained glass");
    await page.getByRole("button", { name: "Disable tree" }).click();
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
        for (const name of ["Stained glass", "Pendants"]) {
            await tree
                .getByRole("treeitem", { name, exact: true })
                .locator("[data-toggle]")
                .first()
                .tap();
        }
        const leaf = tree.getByRole("treeitem", { name: /^A collection of long/ });
        await leaf.tap();
        await expect(leaf).toHaveAttribute("aria-selected", "true");
        expect(await tree.evaluate(node => node.scrollWidth <= node.clientWidth)).toBe(true);
    });
});
