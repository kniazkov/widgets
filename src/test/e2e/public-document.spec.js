import { test, expect } from "@playwright/test";

for (const path of ["public-catalog", "public-terms"]) {
    test(`${path}: anonymous initial render matches interactive public content`, async ({
        page
    }, info) => {
        let release;
        const gate = new Promise(resolve => (release = resolve));
        await page.route("**/*", async route => {
            if (route.request().method() === "POST") await gate;
            await route.continue();
        });
        try {
            await page.goto(`/${path}`);
            const initial = page.locator("#widgets-initial .widgets-public");
            await expect(initial).toBeVisible();
            const before = await initial.evaluate(node => ({
                html: node.outerHTML,
                rect: {
                    x: node.getBoundingClientRect().x,
                    y: node.getBoundingClientRect().y,
                    width: node.getBoundingClientRect().width,
                    height: node.getBoundingClientRect().height
                }
            }));
            await page.screenshot({ path: info.outputPath("initial.png") });
            const title = await page.title();
            release();
            await expect(page.locator("#widgets-initial")).toHaveCount(0);
            await expect(page.getByRole("button", { name: "Try interaction" })).toBeVisible();
            const content = page.locator(".widgets-public");
            await expect(content).toHaveCount(1);
            const after = await content.evaluate(node => ({
                html: node.outerHTML,
                rect: {
                    x: node.getBoundingClientRect().x,
                    y: node.getBoundingClientRect().y,
                    width: node.getBoundingClientRect().width,
                    height: node.getBoundingClientRect().height
                }
            }));
            expect(after).toEqual(before);
            await expect(page).toHaveTitle(title);
            await page.screenshot({ path: info.outputPath("interactive.png") });
            await page.getByRole("button", { name: "Try interaction" }).click();
            await expect(page.getByText("Clicks: 1", { exact: true })).toBeVisible();
            await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
                "href",
                `https://example.com/${path}`
            );
        } finally {
            release();
        }
    });
}

test("public content and links work with JavaScript disabled", async ({ browser, baseURL }) => {
    const context = await browser.newContext({ javaScriptEnabled: false, baseURL });
    const page = await context.newPage();
    try {
        await page.goto("/public-catalog");
        await expect(
            page.getByRole("heading", { name: "Public catalogue", exact: true })
        ).toBeVisible();
        await expect(page.getByText("Price: 2500 RUB")).toBeVisible();
        await expect(page.getByRole("button")).toHaveCount(0);
        await page.getByRole("link", { name: "Read terms" }).click();
        await expect(page).toHaveTitle("Public terms");
        await expect(page.getByRole("heading", { name: "Section 60", exact: true })).toBeVisible();
        await page.goto("/private-static-test");
        await expect(page.locator(".widgets-public")).toHaveCount(0);
        await expect(page.getByText("Private UI requires interactive authorization")).toHaveCount(
            0
        );
    } finally {
        await context.close();
    }
});

test("navigation clears private metadata and restores public content on Back", async ({ page }) => {
    await page.goto("/public-catalog");
    await expect(page.getByRole("button", { name: "Try interaction" })).toBeVisible();
    await page.getByRole("link", { name: "Read terms" }).click();
    await expect(page).toHaveTitle("Public terms");
    await expect(page.locator(".widgets-public")).toHaveCount(1);
    await page.goBack();
    await expect(page).toHaveTitle("Public catalogue");
    await expect(page.locator('meta[property="og:url"]')).toHaveAttribute(
        "content",
        "https://example.com/public-catalog"
    );
    // Registered private route participates in the same history controller.
    await page.evaluate(() => {
        const link = document.createElement("a");
        link.href = "/private-static-test";
        document.body.append(link);
        link.click();
        link.remove();
    });
    await expect(page.getByText("Private UI requires interactive authorization")).toBeVisible();
    await expect(
        page.locator('.widgets-public, link[rel="canonical"], meta[property^="og:"]')
    ).toHaveCount(0);
    await expect(page).not.toHaveTitle("Public catalogue");
    await page.goBack();
    await expect(page).toHaveTitle("Public catalogue");
    await expect(page.locator(".widgets-public")).toHaveCount(1);
});

test("failed bootstrap leaves readable fallback, early link navigation removes stale fallback", async ({
    page
}) => {
    await page.route("**/*", route =>
        route.request().method() === "POST" ? route.abort() : route.continue()
    );
    await page.goto("/public-catalog");
    await expect(page.locator("#widgets-initial")).toBeVisible();
    await page.unroute("**/*");
    await page.getByRole("link", { name: "Read terms" }).click();
    await expect(page).toHaveTitle("Public terms");
    await expect(page.locator("#widgets-initial")).toHaveCount(0);
    await expect(page.getByRole("heading", { name: "Public catalogue", exact: true })).toHaveCount(
        0
    );
    await expect(page.locator(".widgets-public")).toHaveCount(1);
});
