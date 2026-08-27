import { expect, test } from "@playwright/test";
import { createHash } from "node:crypto";

test("a browser click reaches Java and the resulting update reaches the DOM", async ({ page }) => {
    const pageErrors = [];
    page.on("pageerror", error => pageErrors.push(error.message));

    await page.goto("/");

    await expect(page.getByText("Waiting for browser event", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Run full chain" }).click();
    await expect(page.getByText("Java handled the click", { exact: true })).toBeVisible();
    expect(pageErrors).toEqual([]);
});

function multipartField(body, name) {
    const text = body.toString("latin1");
    const marker = `name="${name}"\r\n\r\n`;
    const start = text.indexOf(marker);
    if (start < 0) {
        return null;
    }
    const valueStart = start + marker.length;
    const end = text.indexOf("\r\n", valueStart);
    return end < 0 ? null : text.slice(valueStart, end);
}

function digest(data) {
    return createHash("sha256").update(data).digest("hex");
}

test("binary uploads retry a lost chunk and preserve round-robin order", async ({ page }) => {
    const pageErrors = [];
    const chunks = [];
    let releaseFirstChunk;
    let reportFirstChunk;
    const firstChunkReleased = new Promise(resolve => {
        releaseFirstChunk = resolve;
    });
    const firstChunkSeen = new Promise(resolve => {
        reportFirstChunk = resolve;
    });

    page.on("pageerror", error => pageErrors.push(error.message));
    await page.route("**/*", async route => {
        const request = route.request();
        const body = request.postDataBuffer();
        if (body && multipartField(body, "action") === "upload chunk") {
            chunks.push({
                fileId: Number(multipartField(body, "fileId")),
                chunkIndex: Number(multipartField(body, "chunkIndex"))
            });
            if (chunks.length === 1) {
                reportFirstChunk();
                await firstChunkReleased;
                await route.abort("failed");
                return;
            }
        }
        await route.continue();
    });

    await page.goto("/");
    const first = Buffer.alloc(64 * 1024 + 17);
    const second = Buffer.alloc(64 * 1024 + 9);
    for (let index = 0; index < first.length; index++) {
        first[index] = index % 256;
    }
    for (let index = 0; index < second.length; index++) {
        second[index] = 255 - (index % 256);
    }
    const chooserPromise = page.waitForEvent("filechooser");
    await page.getByRole("button", { name: "Upload binary files" }).click();
    const chooser = await chooserPromise;
    await chooser.setFiles([
        { name: "first.bin", mimeType: "application/octet-stream", buffer: first },
        { name: "second.bin", mimeType: "application/octet-stream", buffer: second }
    ]);

    await firstChunkSeen;
    await expect(page.getByText("Selected first.bin 0%", { exact: true })).toBeVisible();
    await expect(page.getByText("Selected second.bin 0%", { exact: true })).toBeVisible();
    releaseFirstChunk();

    await expect(
        page.getByText(`Loaded first.bin 100% ${digest(first)}`, { exact: true })
    ).toBeVisible();
    await expect(
        page.getByText(`Loaded second.bin 100% ${digest(second)}`, { exact: true })
    ).toBeVisible();
    expect(chunks.slice(0, 5)).toEqual([
        { fileId: 1, chunkIndex: 0 },
        { fileId: 2, chunkIndex: 0 },
        { fileId: 1, chunkIndex: 0 },
        { fileId: 2, chunkIndex: 1 },
        { fileId: 1, chunkIndex: 1 }
    ]);
    expect(pageErrors).toEqual([]);
});
