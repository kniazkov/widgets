import { expect, test } from "@playwright/test";

const fixtureURL = process.env.WIDGETS_UPLOAD_LIMIT_BASE_URL;
const limit = 1024 * 1024;

test("maximum chunk uploads, plus one byte is explained, and next upload still works", async ({
    page
}) => {
    await page.goto(fixtureURL);
    const loader = page.getByRole("button", { name: "Upload sample file" });
    const notice = page.getByRole("status");
    let completedChunks = 0;
    page.on("response", async response => {
        if (response.request().method() !== "POST" || response.status() !== 200) return;
        try {
            const receipt = await response.json();
            if (receipt.result === true && receipt.nextChunk === 1 && receipt.complete === true) {
                completedChunks++;
            }
        } catch {
            // An unrelated response is not a binary upload receipt.
        }
    });
    async function choose(name, size) {
        const [picker] = await Promise.all([page.waitForEvent("filechooser"), loader.click()]);
        await picker.setFiles({
            name,
            mimeType: "application/octet-stream",
            buffer: Buffer.alloc(size, 65)
        });
    }

    await choose("at-limit.bin", limit);
    await expect(page.getByText("Loaded at-limit.bin (1048576 bytes)")).toBeVisible();
    await expect.poll(() => completedChunks).toBe(1);
    await expect(notice).toHaveCount(0);

    await choose("over-limit.bin", limit + 1);
    await expect(notice).toHaveAttribute("data-upload-error", "fileTooLarge");
    await expect(notice).toHaveText("File is too large. Maximum file size: 1048576 bytes.");
    await expect.poll(() => completedChunks).toBe(1);
    await expect(page.getByText("Loaded at-limit.bin (1048576 bytes)")).toBeVisible();

    await choose("recovery.bin", 7);
    await expect(page.getByText("Loaded recovery.bin (7 bytes)")).toBeVisible();
    await expect(notice).toHaveCount(0);
    await expect.poll(() => completedChunks).toBe(2);
});
