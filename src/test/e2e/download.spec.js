import { expect, test } from "@playwright/test";
import { readFileSync } from "node:fs";

test("PDF скачивается без публичного URL и перехода со страницы", async ({ page }) => {
    await page.setContent('<button id="download">PDF</button>');
    await page.addScriptTag({
        content: readFileSync("src/main/html/scripts/client/lifecycle.js", "utf8")
    });
    await page.evaluate(() => {
        document.querySelector("button").onclick = () =>
            setTimeout(
                () =>
                    downloadFile({
                        name: "cdek-123.pdf",
                        contentType: "application/pdf",
                        content: "JVBERi0xLjQK"
                    }),
                10
            );
    });
    const pending = page.waitForEvent("download");
    await page.getByRole("button").click();
    const download = await pending;
    expect(download.suggestedFilename()).toBe("cdek-123.pdf");
    expect(await download.failure()).toBeNull();
    const stream = await download.createReadStream();
    const chunks = [];
    for await (const chunk of stream) chunks.push(chunk);
    expect(Buffer.concat(chunks).toString()).toBe("%PDF-1.4\n");
    await expect(page.getByRole("button")).toBeVisible();
});
