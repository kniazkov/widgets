import { test, expect } from "@playwright/test";

for (const stale of ["missing", "different"]) {
    test(`stale client (${stale}) cannot create a UI and recovers after explicit reload`, async ({
        page
    }) => {
        const replies = [];
        page.on("response", async response => {
            if (response.request().method() === "POST") {
                const body = await response.json().catch(() => null);
                if (body) replies.push(body);
            }
        });
        await page.route("**/scripts/page-runtime.js?*", async route => {
            const response = await route.fetch();
            const original = await response.text();
            expect(original).toContain("request.clientBuild = clientBuildIdentity();");
            await route.fulfill({
                response,
                body: original.replace(
                    "request.clientBuild = clientBuildIdentity();",
                    stale === "missing"
                        ? "/* pre-R39 client: no bundle identity */"
                        : 'request.clientBuild = "old-build";'
                )
            });
        });
        await page.goto("/public-catalog");
        await expect(page.locator('#client-error-overlay[data-reason="version"]')).toBeVisible();
        await expect(page.getByText("Widgets was updated.", { exact: false })).toBeVisible();
        await expect(page.getByRole("button", { name: "Try interaction" })).toHaveCount(0);
        await expect(page.locator("#widgets-initial")).toBeVisible();
        await expect.poll(() => replies.some(reply => reply.clientBuildMismatch)).toBe(true);
        expect(replies.some(reply => typeof reply.id === "string")).toBe(false);
        await page.unroute("**/scripts/page-runtime.js?*");
        await page.reload();
        await expect(page.getByRole("button", { name: "Try interaction" })).toBeVisible();
        await page.getByRole("button", { name: "Try interaction" }).click();
        await expect(page.getByText("Clicks: 1", { exact: true })).toBeVisible();
        await expect(page.locator("#client-error-overlay")).toHaveCount(0);
    });
}

test("mixed cached runtime and current transport cannot borrow each other's build identity", async ({
    page
}) => {
    await page.route("**/scripts/page-runtime.js?*", async route => {
        const response = await route.fetch();
        const body = await response.text();
        expect(body).toMatch(/const widgetsRuntimeBuild = "[a-f0-9]{64}";/);
        await route.fulfill({
            response,
            body: body.replace(
                /const widgetsRuntimeBuild = "[a-f0-9]{64}";/,
                'const widgetsRuntimeBuild = "stale-runtime";'
            )
        });
    });
    await page.goto("/public-catalog");
    await expect(page.locator('#client-error-overlay[data-reason="version"]')).toBeVisible();
    await expect(page.getByRole("button", { name: "Try interaction" })).toHaveCount(0);
});
