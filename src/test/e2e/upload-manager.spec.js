import { mkdir, writeFile } from "node:fs/promises";
import { expect, test } from "@playwright/test";

async function select(page, files) {
    const chooser = page.waitForEvent("filechooser");
    await page.getByRole("button", { name: "Upload managed files", exact: true }).click();
    await (
        await chooser
    ).setFiles(
        files.map(name => ({ name, mimeType: "text/plain", buffer: Buffer.alloc(100000, 7) }))
    );
}

test("managed files isolate errors and retry with one logical entry", async ({ page }) => {
    await page.goto("/all-widgets");
    await select(page, ["first.txt", "invalid.exe", "retry.txt"]);
    const list = page.locator(".widgets-upload-list");
    await expect(list.locator('[data-upload-state="done"]')).toContainText("first.txt");
    await expect(list.locator('[data-upload-state="error"]')).toHaveCount(2);
    await page.getByRole("button", { name: "retry retry.txt", exact: true }).focus();
    await page.keyboard.press("Enter");
    await expect(list.locator('[data-upload-state="done"]')).toHaveCount(2);
    await expect(list.locator(".widgets-upload-row").filter({ hasText: "retry.txt" })).toHaveCount(
        1
    );
    await page.getByRole("button", { name: "remove invalid.exe", exact: true }).click();
    await expect(list.locator(".widgets-upload-row")).toHaveCount(2);
});

test("cancelled transfer can be retried without retaining an extra row", async ({ page }) => {
    await blockSecondChunk(page);
    await page.goto("/all-widgets");
    await select(page, ["cancel.txt"]);
    await expect.poll(() => blocked(page)).toBe(true);
    const row = page.locator(".widgets-upload-row").filter({ hasText: "cancel.txt" });
    await page.getByRole("button", { name: "cancel cancel.txt", exact: true }).click();
    await expect(row).toHaveAttribute("data-upload-state", "cancelled");
    await release(page);
    await page.getByRole("button", { name: "retry cancel.txt", exact: true }).click();
    await expect(row).toHaveAttribute("data-upload-state", "done");
    await expect(page.locator(".widgets-upload-row")).toHaveCount(1);
});

async function blockSecondChunk(page) {
    await page.addInitScript(() => {
        const send = XMLHttpRequest.prototype.send;
        window.uploadBlocked = false;
        XMLHttpRequest.prototype.send = function (body) {
            if (
                body instanceof FormData &&
                body.get("action") === "upload chunk" &&
                body.get("fileId") === "1" &&
                body.get("chunkIndex") === "1"
            ) {
                window.uploadBlocked = true;
                window.releaseUpload = () => send.call(this, body);
                return;
            }
            return send.call(this, body);
        };
    });
}
async function blocked(page) {
    return (
        await Promise.all(page.frames().map(f => f.evaluate(() => window.uploadBlocked || false)))
    ).some(Boolean);
}
async function release(page) {
    await Promise.all(page.frames().map(f => f.evaluate(() => window.releaseUpload?.())));
}

test("lost final chunk reply does not prevent other files completing", async ({ page }) => {
    await page.addInitScript(() => {
        const send = XMLHttpRequest.prototype.send;
        let discarded = false;
        XMLHttpRequest.prototype.send = function (body) {
            if (
                !discarded &&
                body instanceof FormData &&
                body.get("action") === "upload chunk" &&
                body.get("fileId") === "1" &&
                body.get("chunkIndex") === "1"
            ) {
                discarded = true;
                const handler = this.onreadystatechange;
                this.onreadystatechange = function (...args) {
                    if (this.readyState === 4) Object.defineProperty(this, "status", { value: 0 });
                    return handler.apply(this, args);
                };
            }
            return send.call(this, body);
        };
    });
    await page.goto("/all-widgets");
    await select(page, ["network.txt", "other.txt"]);
    await expect(page.locator('[data-upload-state="done"]')).toHaveCount(2);
    await expect(page.locator(".widgets-upload-row")).toHaveCount(2);
});

for (const [label, viewport] of [
    ["desktop", { width: 1365, height: 900 }],
    ["mobile", { width: 390, height: 844 }]
]) {
    test(`upload gallery ${label}`, async ({ page }, testInfo) => {
        test.skip(!process.env.WIDGETS_CAPTURE_UPLOADS, "Explicit gallery capture only");
        await page.setViewportSize(viewport);
        await blockSecondChunk(page);
        await page.goto("/all-widgets");
        await select(page, ["progress.txt", "invalid.exe", "retry.txt"]);
        await expect.poll(() => blocked(page)).toBe(true);
        const list = page.locator(".widgets-upload-list");
        await expect(list.locator('[data-upload-state="error"]')).toHaveCount(1);
        const card = page
            .getByRole("heading", { name: "UploadManager / UploadList", exact: true })
            .locator("../..");
        const capture = async state => {
            await card.scrollIntoViewIfNeeded();
            const box = await card.boundingBox();
            const scroll = await page.evaluate(() => ({ x: scrollX, y: scrollY }));
            const directory = `target/upload-gallery/${testInfo.project.name}`;
            await mkdir(directory, { recursive: true });
            const name = `upload-${label}-${state}`;
            await page.screenshot({ path: `${directory}/${name}.png`, fullPage: true });
            await writeFile(
                `${directory}/${name}.json`,
                JSON.stringify({ viewport, box, scroll, browser: testInfo.project.name })
            );
        };
        await capture("progress");
        await release(page);
        await expect(list.locator('[data-upload-state="done"]')).toHaveCount(1);
        await expect(list.locator('[data-upload-state="error"]')).toHaveCount(2);
        await capture("error");
        await page.getByRole("button", { name: "retry retry.txt", exact: true }).click();
        await expect(list.locator('[data-upload-state="done"]')).toHaveCount(2);
        await capture("done");
    });
}

test.describe("touch upload controls", () => {
    test.use({ hasTouch: true, viewport: { width: 390, height: 844 } });
    test("removes a completed file with a touch action", async ({ page }) => {
        await page.goto("/all-widgets");
        const chooser = page.waitForEvent("filechooser");
        await page.getByRole("button", { name: "Upload managed files", exact: true }).tap();
        await (
            await chooser
        ).setFiles({ name: "touch.txt", mimeType: "text/plain", buffer: Buffer.from("touch") });
        await expect(page.locator('[data-upload-state="done"]')).toHaveCount(1);
        await page.getByRole("button", { name: "remove touch.txt", exact: true }).tap();
        await expect(page.locator(".widgets-upload-row")).toHaveCount(0);
    });
});
