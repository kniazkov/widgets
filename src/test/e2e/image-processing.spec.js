import { expect, test } from "@playwright/test";
import { fileURLToPath } from "node:url";

test("progress precedes a bounded decode result and a rejected image can be followed by success", async ({
    page
}) => {
    await page.goto("/bounded-image-processing");
    const button = page.getByRole("button", { name: "Choose image sample" });
    const choose = async size => {
        const [picker] = await Promise.all([page.waitForEvent("filechooser"), button.click()]);
        await picker.setFiles(
            fileURLToPath(
                new URL("../resources/image-processing/image-" + size + ".png", import.meta.url)
            )
        );
    };
    await choose(16);
    await expect(page.getByText("Image rejected by decoder policy")).toBeVisible();
    await expect(page.getByText("Upload progress 100%")).toBeVisible();

    await choose(8);
    await expect(page.getByText("Decoded 8 x 8")).toBeVisible();
    await expect(page.getByText("Upload progress 100%")).toBeVisible();
    await expect(page.getByText("Image rejected by decoder policy")).toHaveCount(0);
});

test("an overfull processor refuses the final chunk and recovers on the next selection", async ({
    page
}) => {
    const url = process.env.WIDGETS_UPLOAD_LIMIT_BASE_URL;
    await page.goto(url + "/bounded-processing-queue");
    const button = page.getByRole("button", { name: "Choose queue sample" });
    const choose = async (...names) => {
        const [picker] = await Promise.all([page.waitForEvent("filechooser"), button.click()]);
        await picker.setFiles(
            names.map(name => ({
                name,
                mimeType: "application/octet-stream",
                buffer: Buffer.from("ordinary small fixture")
            }))
        );
    };
    await choose("running.bin");
    await expect(page.getByText("Active running.bin")).toBeVisible();
    await choose("queued.bin", "refused.bin");
    await expect(page.getByRole("status")).toHaveText(
        "File processing is busy. Select the file again later."
    );
    await page.getByRole("button", { name: "Release processor" }).click();
    await expect(page.getByText("Processed queued.bin")).toBeVisible();
    await choose("next.bin");
    await expect(page.getByText("Processed next.bin")).toBeVisible();
    await expect(page.getByRole("status")).toHaveCount(0);
});
