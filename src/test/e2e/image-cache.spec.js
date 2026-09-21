import { expect, test } from "@playwright/test";

for (const mobile of [false, true]) {
    test(`JavaScript-created images reuse browser cache (mobile=${mobile})`, async ({
        browser,
        baseURL
    }) => {
        const context = await browser.newContext({
            viewport: { width: mobile ? 390 : 1200, height: 844 },
            isMobile: mobile,
            hasTouch: mobile
        });
        try {
            const page = await context.newPage();
            const cdp = await context.newCDPSession(page);
            await cdp.send("Network.enable");
            const images = new Set();
            const transferred = [];
            cdp.on("Network.requestWillBeSent", event => {
                if (event.request.url.includes("/cache-images/logo.svg")) {
                    images.add(event.requestId);
                }
            });
            cdp.on("Network.responseReceivedExtraInfo", event => {
                if (images.has(event.requestId)) transferred.push(event.statusCode);
            });
            await page.goto(`${baseURL}/image-cache`);
            const logo = page.locator('img[src*="/cache-images/logo.svg"]');
            await expect(logo).toBeVisible();
            await logo.evaluate(image => image.decode());
            const source = await logo.getAttribute("src");
            expect(source).toContain("widgets-version=");
            await expect.poll(() => transferred).toEqual([200]);
            await expect(logo).toHaveAttribute("width", "350");
            for (let index = 0; index < 2; index++) {
                const previous = await logo.elementHandle();
                await page.getByRole("button", { name: "Recreate logo" }).click();
                await expect.poll(() => previous.evaluate(image => image.isConnected)).toBe(false);
                await logo.evaluate(image => image.decode());
                await expect(logo).toHaveAttribute("src", source);
                await previous.dispose();
            }
            await cdp.send("Runtime.evaluate", { expression: "0" });
            expect(transferred).toEqual([200]);
        } finally {
            await context.close();
        }
    });
}

test("a delayed logo reserves the same box before and after loading", async ({ page }) => {
    let release;
    const gate = new Promise(resolve => {
        release = resolve;
    });
    await page.route("**/cache-images/logo.svg?*", async route => {
        await gate;
        await route.continue();
    });
    try {
        await page.goto("/image-cache");
        const logo = page.locator('img[src*="/cache-images/logo.svg"]');
        const text = page.getByText("Below logo", { exact: true });
        await expect(text).toBeVisible();
        await expect(logo).toHaveAttribute("width", "350");
        expect(await logo.evaluate(image => image.complete)).toBe(false);
        const before = { image: await logo.boundingBox(), text: await text.boundingBox() };
        expect(before.image.width).toBeGreaterThan(0);
        expect(before.image.height).toBeGreaterThan(0);
        release();
        await logo.evaluate(image => image.decode());
        expect({ image: await logo.boundingBox(), text: await text.boundingBox() }).toEqual(before);
        expect(await logo.evaluate(image => getComputedStyle(image).objectFit)).toBe("contain");
    } finally {
        release();
    }
});
