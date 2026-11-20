import { expect, test } from "@playwright/test";

async function select(page, name, size) {
    const chosen = page.waitForEvent("filechooser");
    await page.getByRole("button", { name: "Upload binary files", exact: true }).click();
    await (
        await chosen
    ).setFiles({ name, mimeType: "application/octet-stream", buffer: Buffer.alloc(size, 7) });
}

test("cancel releases an interrupted upload and permits the next file", async ({ page }) => {
    await page.addInitScript(() => {
        const send = XMLHttpRequest.prototype.send;
        window.uploadBlocked = false;
        window.releaseUpload = () => {};
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
    await page.goto("/");
    await select(page, "cancel.bin", 128 * 1024);
    await expect
        .poll(async () =>
            (
                await Promise.all(
                    page.frames().map(frame => frame.evaluate(() => window.uploadBlocked || false))
                )
            ).some(Boolean)
        )
        .toBe(true);
    await expect(page.getByText("Selected cancel.bin 50%", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Cancel cancel.bin", exact: true }).click();
    await expect(page.getByText("Cancelled cancel.bin", { exact: true })).toBeVisible();
    await Promise.all(page.frames().map(frame => frame.evaluate(() => window.releaseUpload?.())));
    await select(page, "next.bin", 123);
    await expect(page.getByText(/^Loaded next.bin 100%/)).toBeVisible();
    await expect(page.getByText(/^Loaded cancel.bin/)).toHaveCount(0);
});

test("lost final reply retries after callback resources have been released", async ({ page }) => {
    await page.addInitScript(() => {
        const send = XMLHttpRequest.prototype.send;
        window.uploadAttempts = 0;
        window.uploadReplies = [];
        XMLHttpRequest.prototype.send = function (body) {
            if (
                body instanceof FormData &&
                body.get("action") === "upload chunk" &&
                body.get("fileId") === "1"
            ) {
                const attempt = ++window.uploadAttempts;
                const handler = this.onreadystatechange;
                this.onreadystatechange = function (...args) {
                    if (this.readyState === 4) {
                        window.uploadReplies.push(JSON.parse(this.responseText));
                        // Discard the real server reply only at the browser delivery boundary.
                        // WebKit's intercepted postDataBuffer omits file bytes; keep native sending.
                        if (attempt === 1) Object.defineProperty(this, "status", { value: 0 });
                    }
                    return handler.apply(this, args);
                };
            }
            return send.call(this, body);
        };
    });
    await page.goto("/");
    await select(page, "receipt.bin", 123);
    await expect(page.getByText(/^Loaded receipt.bin 100%/)).toBeVisible();
    await expect
        .poll(async () => {
            const attempts = await Promise.all(
                page.frames().map(frame => frame.evaluate(() => window.uploadAttempts || 0))
            );
            return attempts.reduce((sum, value) => sum + value, 0);
        })
        .toBe(2);
    const replies = (
        await Promise.all(
            page.frames().map(frame => frame.evaluate(() => window.uploadReplies || []))
        )
    ).flat();
    expect(replies).toHaveLength(2);
    expect(replies.every(reply => reply.result === true && reply.complete === true)).toBe(true);
    await select(page, "following.bin", 123);
    await expect(page.getByText(/^Loaded following.bin 100%/)).toBeVisible();
});
