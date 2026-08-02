import { createHash } from "node:crypto";

import { expect, test } from "@playwright/test";

test("a browser click reaches Java and the resulting update reaches the DOM", async ({ page }) => {
    const pageErrors = [];
    page.on("pageerror", error => pageErrors.push(error.message));

    await page.goto("/");

    await expect(page.getByText("Waiting for browser event", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Run full chain" }).click();
    await expect(page.getByText("Java handled the click", { exact: true })).toBeVisible();
    expect(pageErrors).toEqual([]);
});

test("a boundary-sized binary file reaches Java intact in one chunk", async ({ page }) => {
    const pageErrors = [];
    page.on("pageerror", error => pageErrors.push(error.message));
    const content = Buffer.alloc(64 * 1024);
    for (let index = 0; index < content.length; index++) {
        content[index] = (index * 131 + 17) & 0xff;
    }
    const sha256 = createHash("sha256").update(content).digest("hex");

    await page.goto("/");
    const [chooser] = await Promise.all([
        page.waitForEvent("filechooser"),
        page.getByRole("button", { name: "Upload binary file" }).click()
    ]);
    await chooser.setFiles({
        name: "boundary.bin",
        mimeType: "application/octet-stream",
        buffer: content
    });

    await expect(
        page.getByText(`Uploaded boundary.bin: 65536 bytes, sha256=${sha256}, chunks=1`, {
            exact: true
        })
    ).toBeVisible();
    expect(pageErrors).toEqual([]);
});
