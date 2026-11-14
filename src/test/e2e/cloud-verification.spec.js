import { expect, test } from "@playwright/test";

test("AllWidgets desktop accepts keyboard, popup, carousel and upload actions", async ({
    page
}) => {
    await page.goto("/all-widgets");
    const suggestion = page.getByRole("combobox").first();
    await suggestion.fill("rhodium");
    await suggestion.press("ArrowDown");
    await suggestion.press("Enter");
    await expect(suggestion).toHaveValue("Rhodium-plated brass");

    await page.getByRole("button", { name: "Show message", exact: true }).click();
    const dialog = page.getByRole("dialog");
    await expect(dialog).toContainText("The operation completed successfully.");
    await dialog.getByRole("button", { name: "Close", exact: true }).click();
    await expect(dialog).toHaveCount(0);

    const carousel = page.locator(".carousel");
    await carousel.scrollIntoViewIfNeeded();
    const box = await carousel.boundingBox();
    expect(box).not.toBeNull();
    const y = box.y + box.height * 0.5;
    await page.mouse.move(box.x + box.width * 0.8, y);
    await page.mouse.down();
    await page.mouse.move(box.x + box.width * 0.2, y, { steps: 4 });
    await page.mouse.up();
    await expect(page.getByText("Selected image: 2", { exact: true })).toBeVisible();

    const chooser = page.waitForEvent("filechooser");
    await page.getByRole("button", { name: "Choose file", exact: true }).click();
    await (
        await chooser
    ).setFiles({
        name: "cloud-smoke.txt",
        mimeType: "text/plain",
        buffer: Buffer.from("Widgets")
    });
    await expect(
        page.getByText("File uploaded: cloud-smoke.txt (7 bytes)", { exact: true })
    ).toBeVisible();
});

test("AllWidgets uses the device viewport and sends a popup event from a touchscreen", async ({
    browser,
    baseURL
}) => {
    const context = await browser.newContext({
        baseURL,
        viewport: { width: 390, height: 844 },
        screen: { width: 390, height: 844 },
        isMobile: true,
        hasTouch: true
    });
    try {
        const page = await context.newPage();
        await page.goto("/all-widgets");
        expect(await page.evaluate(() => document.documentElement.clientWidth)).toBe(390);
        await page.getByRole("button", { name: "Show message", exact: true }).tap();
        const dialog = page.getByRole("dialog");
        await expect(dialog).toContainText("The operation completed successfully.");
        await dialog.getByRole("button", { name: "Close", exact: true }).tap();
        await expect(dialog).toHaveCount(0);
    } finally {
        await context.close();
    }
});
