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
        const cancel = page.getByRole("button", { name: "Cancel upload", exact: true });
        await expect(cancel).toBeVisible();
        const region = cancel.locator("xpath=ancestor::*[contains(@class,'widgets-feedback')][1]");
        await expect(region.getByRole("progressbar")).toHaveAttribute("value", "40");
        if (width === 390) await cancel.tap();
        else await cancel.click();
        await expect(region.getByText("Upload cancelled", { exact: true })).toBeVisible();
        await expect(cancel).toBeDisabled();
        await expect(region.getByRole("progressbar")).toHaveCount(0);
        await page.getByRole("button", { name: "Show empty result", exact: true }).click();
        await expect(page.getByText("Check complete: no orders", { exact: true })).toBeVisible();
        await page.getByRole("button", { name: "Start check", exact: true }).click();
        await expect(
            page.getByRole("progressbar", { name: "Checking item availability…" })
        ).toBeVisible();
        await page.getByRole("button", { name: "Check failed", exact: true }).click();
        await expect(
            page.getByText("Check failed. Please try again.", { exact: true })
        ).toBeVisible();
        const retry = page.getByRole("button", { name: "Load again", exact: true });
        await retry.click();
        await expect(page.getByText("Items found: 2", { exact: true })).toBeVisible();
        await expect(page.getByText("Check complete", { exact: true })).toBeVisible();
        const notify = page.getByRole("button", { name: "Show notification", exact: true });
        await notify.focus();
        await notify.press("Enter");
        await expect(page.getByText("Item added to the list.", { exact: true })).toBeVisible();
        await expect(notify).toBeFocused();
        await expect(page.getByText("Item added to the list.", { exact: true })).toHaveCount(0, {
            timeout: 9000
        });
        const error = page.getByText("Could not reach the server. Please retry.", {
            exact: true
        });
        await expect(error).toBeVisible();
        await error.locator("..").getByRole("button", { name: "Dismiss notification" }).click();
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
