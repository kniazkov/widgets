import { expect, test } from "@playwright/test";

test("temporary widget detach and page Back preserve the original input node", async ({ page }) => {
    await page.goto("/widget-lifecycle");
    const input = page.getByRole("textbox");
    await input.evaluate(widget => {
        window.__retainedInput = widget;
        widget.value = "local draft";
    });
    await page.getByRole("button", { name: "Detach input" }).click();
    await expect(page.getByText("Input detached", { exact: true })).toBeVisible();
    await expect(input).toHaveCount(0);
    await page.getByRole("button", { name: "Attach input" }).click();
    await expect(input).toHaveValue("local draft");
    expect(await input.evaluate(widget => widget === window.__retainedInput)).toBe(true);
    await page.getByRole("link", { name: "Next lifecycle page" }).click();
    await expect(page).toHaveURL(/\/catalog$/);
    await page.goBack();
    await expect(input).toHaveValue("local draft");
    expect(await input.evaluate(widget => widget === window.__retainedInput)).toBe(true);
});

test("final disposal plateaus retained Chromium nodes across cached navigation", async ({
    page,
    context
}) => {
    await page.goto("/widget-lifecycle");
    const mount = page.getByRole("button", { name: "Mount disposable batch" });
    const dispose = page.getByRole("button", { name: "Dispose batch" });
    const child = page.getByText("Disposable child 60", { exact: true });
    const cdp = await context.newCDPSession(page);
    async function cycle(index) {
        await mount.click();
        await expect(child).toBeVisible();
        await dispose.click();
        await expect(page.getByText("Batches disposed: " + index, { exact: true })).toBeVisible();
        await expect(child).toHaveCount(0);
    }
    async function nodes() {
        await cdp.send("HeapProfiler.collectGarbage");
        return (await cdp.send("Memory.getDOMCounters")).nodes;
    }
    await cycle(1);
    await page.getByRole("link", { name: "Next lifecycle page" }).click();
    await expect(page.getByText("Catalog row 0", { exact: true })).toBeVisible();
    await page.goBack();
    await expect(mount).toBeVisible();
    const baseline = await nodes();
    for (let index = 2; index <= 9; index++) await cycle(index);
    // Reuse the same cached entry: clicking the link again creates another catalog tree.
    await page.goForward();
    await expect(page.getByText("Catalog row 0", { exact: true })).toBeVisible();
    await page.goBack();
    await expect(dispose).toBeVisible();
    // Allow bookkeeping variation, but not even two retained 60-child subtrees.
    expect(await nodes()).toBeLessThan(baseline + 120);
});
