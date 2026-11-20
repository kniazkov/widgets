import { expect, test } from "@playwright/test";

test("discarding a cached page releases its server client and cannot restore its draft", async ({
    page
}) => {
    await page.goto("/application-lifecycle");
    const firstPage = page.getByText(/^Opened page /);
    await expect(firstPage).toBeVisible();
    const first = await firstPage.textContent();
    await page.getByRole("textbox", { name: "Page draft" }).fill("discard this draft");
    await page.getByRole("link", { name: "Open lifecycle monitor" }).click();

    const read = page.getByRole("button", { name: "Read cleanup count" });
    await read.click();
    const status = page.getByText(/^Closed pages /);
    await expect(status).toBeVisible();
    const before = Number((await status.textContent()).split(" ").at(-1));

    await page.getByRole("button", { name: "Release cached pages" }).click();
    await expect
        .poll(async () => {
            await read.click();
            return Number((await status.textContent()).split(" ").at(-1));
        })
        .toBeGreaterThan(before);

    await page.goBack();
    await expect(firstPage).not.toHaveText(first);
    await expect(page.getByRole("textbox", { name: "Page draft" })).toHaveValue("");
});
