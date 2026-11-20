import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

for (const width of [320, 1280]) {
    test.describe(`navigation at ${width}px`, () => {
        test.use({ hasTouch: true });
        test("keyboard activation, retained drafts, reorder, replacement and removal", async ({
            page
        }) => {
            await page.setViewportSize({ width, height: 900 });
            await page.goto("/all-widgets");
            const tabs = page.getByRole("tablist", { name: "Item details" });
            const description = tabs.getByRole("tab", { name: "Item description" });
            const delivery = tabs.getByRole("tab", { name: "Delivery and collection" });
            const editor = page.getByRole("textbox", { name: "Item note" });
            await expect(description).toHaveAttribute("aria-selected", "true");
            await editor.fill("Saved draft");
            await description.focus();
            await description.press("ArrowRight");
            await expect(delivery).toBeFocused();
            await expect(description).toHaveAttribute("aria-selected", "true");
            await delivery.press("Enter");
            await expect(delivery).toHaveAttribute("aria-selected", "true");
            await expect(editor).toBeHidden();
            await expect(
                page.getByRole("tabpanel", { name: "Delivery and collection" })
            ).toBeVisible();
            await delivery.press("Home");
            await expect(description).toBeFocused();
            await description.press("End");
            await expect(delivery).toBeFocused();
            await delivery.press("ArrowRight");
            await expect(description).toBeFocused();
            await description.press("Space");
            await expect(editor).toHaveValue("Saved draft");
            const reorder = page.getByRole("button", { name: "Reorder tabs", exact: true });
            await reorder.click();
            await expect(tabs.getByRole("tab").first()).toHaveText("Delivery and collection");
            await expect(reorder).toBeFocused();
            await expect(description).toHaveAttribute("aria-selected", "true");
            if (width === 320) await delivery.tap();
            else await delivery.click();
            await page.getByRole("button", { name: "Update delivery", exact: true }).click();
            await expect(
                page.getByText("Delivery options updated by the server.", { exact: true })
            ).toBeVisible();
            await page.getByRole("button", { name: "Remove delivery", exact: true }).click();
            await expect(delivery).toHaveCount(0);
            await expect(description).toHaveAttribute("aria-selected", "true");
            await expect(editor).toHaveValue("Saved draft");
            await reorder.click();
            await expect(description).toHaveAttribute("aria-selected", "true");
            await page.getByRole("button", { name: "Update delivery", exact: true }).click();
            await expect(description).toHaveAttribute("aria-selected", "true");
            await expect(tabs.getByRole("tab", { name: "Customer reviews" })).toBeDisabled();
            await expect(tabs.getByRole("tab", { name: "Hidden tab" })).toHaveCount(0);
        });

        test("accordion policies, wrapped breadcrumbs, native links and accessibility", async ({
            page,
            context
        }) => {
            await page.setViewportSize({ width, height: 900 });
            await page.goto("/all-widgets");
            const questions = page.getByRole("group", {
                name: "Frequently asked questions",
                exact: true
            });
            const care = questions.getByRole("button", {
                name: "How to care for handmade items"
            });
            const packaging = questions.getByRole("button", {
                name: "Gift wrapping and special requests"
            });
            await expect(care).toHaveAttribute("aria-expanded", "true");
            await care.focus();
            await care.press("ArrowDown");
            await expect(packaging).toBeFocused();
            await packaging.press("Enter");
            await expect(packaging).toHaveAttribute("aria-expanded", "true");
            await expect(care).toHaveAttribute("aria-expanded", "false");
            await packaging.press("Home");
            await expect(care).toBeFocused();
            await care.press("End");
            await expect(packaging).toBeFocused();
            await packaging.press("Space");
            await expect(packaging).toHaveAttribute("aria-expanded", "false");
            const multiple = page.getByRole("group", {
                name: "Multiple open sections",
                exact: true
            });
            await expect(multiple.getByRole("region")).toHaveCount(2);
            await multiple.getByRole("button", { name: "Item materials" }).click();
            await expect(multiple.getByRole("region")).toHaveCount(1);
            const path = page.getByRole("navigation", { name: "Item path" });
            await expect(path.getByRole("listitem")).toHaveCount(3);
            await expect(path.locator('[aria-current="page"]')).toHaveText(
                "Detailed information about the selected item"
            );
            await expect(path.getByRole("link")).toHaveCount(2);
            await expect(path.getByRole("link", { name: "Home", exact: true })).toHaveAttribute(
                "href",
                "/"
            );
            const card = path.locator("..");
            expect(await card.evaluate(el => el.scrollWidth <= el.clientWidth)).toBe(true);
            const bounds = await card.evaluate(el => {
                const host = el.getBoundingClientRect();
                return Array.from(
                    el.querySelectorAll('[role="tab"], button[aria-expanded], input')
                ).every(control => {
                    if (!control.getClientRects().length) return true;
                    const box = control.getBoundingClientRect();
                    if (box.right > host.right || box.left < host.left) return false;
                    return Array.from(control.children).every(child => {
                        const text = child.getBoundingClientRect();
                        return text.bottom <= box.bottom && text.right <= box.right;
                    });
                });
            });
            expect(bounds).toBe(true);
            // Axe opens/closes a helper tab. Exercise native popup creation before that lifecycle
            // so the assertion only observes the user link, not the accessibility helper page.
            const nextPage = context.waitForEvent("page");
            await path
                .getByRole("link", { name: "Home", exact: true })
                .click({ modifiers: ["ControlOrMeta"] });
            const opened = await nextPage;
            await expect(opened).toHaveURL(/\/$/);
            await opened.close();
            expect(
                (
                    await new AxeBuilder({ page })
                        .include(".widgets-tabs")
                        .include(".widgets-accordion")
                        .include(".widgets-breadcrumbs")
                        .analyze()
                ).violations
            ).toEqual([]);
        });
    });
}

test("Back and Forward retain selected tab and draft", async ({ page }) => {
    await page.goto("/all-widgets");
    const editor = page.getByRole("textbox", { name: "Item note" });
    await editor.fill("Browser history");
    const delivery = page.getByRole("tab", { name: "Delivery and collection" });
    await delivery.click();
    await expect(delivery).toHaveAttribute("aria-selected", "true");
    await page
        .getByRole("navigation", { name: "Item path" })
        .getByRole("link", { name: "Home", exact: true })
        .click();
    await expect(page).toHaveURL(/\/$/);
    await page.goBack();
    await expect(delivery).toHaveAttribute("aria-selected", "true");
    await page.getByRole("tab", { name: "Item description" }).click();
    await expect(editor).toHaveValue("Browser history");
    await page.goForward();
    await expect(page).toHaveURL(/\/$/);
    await page.goBack();
    await expect(editor).toHaveValue("Browser history");
});
