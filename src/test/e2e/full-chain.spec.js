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

test("persistent connection loss blocks the page until the server responds", async ({ page }) => {
    let blockSynchronization = false;
    await page.route("**/*", async route => {
        const request = route.request();
        const body = request.postDataBuffer();
        if (blockSynchronization && body && multipartField(body, "action") === "synchronize") {
            await route.abort("failed");
            return;
        }
        await route.continue();
    });

    await page.goto("/");
    await expect(page.getByText("Waiting for browser event", { exact: true })).toBeVisible();

    blockSynchronization = true;
    await page.evaluate(() => {
        mainCycle();
        mainCycle();
        mainCycle();
    });
    await expect(page.getByText("Connection terminated", { exact: true })).toBeVisible();

    blockSynchronization = false;
    await page.evaluate(() => mainCycle());
    await expect(page.getByText("Connection terminated", { exact: true })).toBeHidden();
});

test("a dead client causes the browser to reload the same page", async ({ page }) => {
    let rejectNextSynchronization = false;
    const currentServer = { id: null };
    await page.route("**/*", async route => {
        const request = route.request();
        const body = request.postDataBuffer();
        if (rejectNextSynchronization && body && multipartField(body, "action") === "synchronize") {
            rejectNextSynchronization = false;
            await route.fulfill({
                contentType: "application/json",
                body: JSON.stringify({
                    result: false,
                    clientAlive: false,
                    serverId: currentServer.id
                })
            });
            return;
        }
        await route.continue();
    });

    await page.goto("/?item=42");
    await expect(page.getByText("Waiting for browser event", { exact: true })).toBeVisible();
    currentServer.id = await page.evaluate(() => serverId);
    const previousClientId = await page.evaluate(() => clientId);

    rejectNextSynchronization = true;
    const navigation = page.waitForEvent("framenavigated", {
        predicate: frame => frame === page.mainFrame()
    });
    await page.evaluate(() => mainCycle());
    await navigation;

    await expect(page.getByText("Waiting for browser event", { exact: true })).toBeVisible();
    expect(await page.evaluate(() => clientId)).not.toBe(previousClientId);
    expect(new URL(page.url()).search).toBe("?item=42");
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
    let releaseRetry;
    let reportRetry;
    let blockSynchronize = false;
    const firstChunkReleased = new Promise(resolve => {
        releaseFirstChunk = resolve;
    });
    const firstChunkSeen = new Promise(resolve => {
        reportFirstChunk = resolve;
    });
    const retryReleased = new Promise(resolve => {
        releaseRetry = resolve;
    });
    const retrySeen = new Promise(resolve => {
        reportRetry = resolve;
    });

    page.on("pageerror", error => pageErrors.push(error.message));
    await page.route("**/*", async route => {
        const request = route.request();
        const body = request.postDataBuffer();
        const target = new URL(request.url());
        const action =
            target.searchParams.get("action") || (body && multipartField(body, "action"));
        if (action === "synchronize" && blockSynchronize) {
            await route.abort("failed");
            return;
        }
        if (action === "upload chunk") {
            const field = name =>
                target.searchParams.get(name) || (body && multipartField(body, name));
            const encodedChunk = field("chunk");
            chunks.push({
                fileId: Number(field("fileId")),
                chunkIndex: Number(field("chunkIndex")),
                lastUpdate: field("lastUpdate"),
                contentType: request.headers()["content-type"],
                encodedChunk,
                hasQuery: target.search.length > 0
            });
            if (chunks.length === 1) {
                blockSynchronize = true;
                reportFirstChunk();
                await firstChunkReleased;
                await route.abort("failed");
                return;
            }
            if (chunks.length === 3) {
                reportRetry();
                await retryReleased;
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

    await retrySeen;
    await expect(page.getByText("Selected first.bin 0%", { exact: true })).toBeVisible();
    await expect(page.getByText("Selected second.bin 50%", { exact: true })).toBeVisible();
    expect(chunks[2].lastUpdate).not.toBe(chunks[1].lastUpdate);
    blockSynchronize = false;
    releaseRetry();

    await expect(
        page.getByText(`Loaded first.bin 100% ${digest(first)}`, { exact: true })
    ).toBeVisible();
    await expect(
        page.getByText(`Loaded second.bin 100% ${digest(second)}`, { exact: true })
    ).toBeVisible();
    expect(chunks.slice(0, 5).map(({ fileId, chunkIndex }) => ({ fileId, chunkIndex }))).toEqual([
        { fileId: 1, chunkIndex: 0 },
        { fileId: 2, chunkIndex: 0 },
        { fileId: 1, chunkIndex: 0 },
        { fileId: 2, chunkIndex: 1 },
        { fileId: 1, chunkIndex: 1 }
    ]);
    expect(chunks.every(chunk => chunk.contentType.startsWith("multipart/form-data;"))).toBe(true);
    expect(chunks.every(chunk => !chunk.hasQuery)).toBe(true);
    expect(chunks[0].encodedChunk).toHaveLength(2 * 64 * 1024);
    expect(chunks[1].encodedChunk).toHaveLength(2 * 64 * 1024);
    expect(pageErrors).toEqual([]);
});
