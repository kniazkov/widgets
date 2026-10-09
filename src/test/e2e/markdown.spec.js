import { test, expect } from "@playwright/test";

for (const width of [320, 1280]) {
    test(`Markdown updates and scales at ${width}px`, async ({ page }) => {
        await page.setViewportSize({ width, height: 844 });
        await page.goto("/markdown");
        const document = page.locator(".widgets-markdown");
        await expect(document.getByRole("heading", { name: "Terms", exact: true })).toBeVisible();
        await expect(document.locator("strong")).toHaveText("carefully");
        await page.getByRole("button", { name: "Update document" }).click();
        await expect(document.getByRole("heading", { name: "Updated terms" })).toHaveCSS(
            "font-size",
            "32px"
        );
        await expect(document).toHaveCSS("font-size", "20px");
        await expect(document).toHaveCSS("font-family", "Georgia, serif");
        await expect(document.locator("ol > li")).toHaveCount(2);
        const long = "LongUnbrokenWord".repeat(80);
        const source = `# Edited\n\n**From editor**\n\n${long}\n\n\`\`\`\n${long}\n\`\`\`\n\n<img src=x onerror=alert(1)>`;
        await page.getByRole("textbox").fill(source);
        await expect(document.locator("strong")).toHaveText("From editor");
        await expect(document.locator("img")).toHaveCount(0);
        const dimensions = await document.evaluate(el => ({
            width: el.clientWidth,
            scroll: el.scrollWidth
        }));
        expect(dimensions.scroll).toBeLessThanOrEqual(dimensions.width + 1);
        await page.getByRole("textbox").fill("");
        await expect(document).toBeEmpty();
    });
}
