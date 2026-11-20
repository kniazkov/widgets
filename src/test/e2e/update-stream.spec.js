import { expect, test } from "@playwright/test";

test("unsupported update blocks later state and reload builds a fresh snapshot", async ({
    page
}) => {
    await page.goto("/update-stream");
    const increment = page.getByRole("button", { name: "Increment model" });
    await expect(page.getByText("Model count: 0")).toBeVisible();
    await increment.click();
    await expect(page.getByText("Model count: 1")).toBeVisible();

    await page.getByRole("button", { name: "Simulate unsupported update" }).click();
    const error = page.getByRole("alert").filter({ hasText: "Client Error" });
    await expect(error).toHaveAttribute("data-reason", "protocol");
    await expect(error).toContainText("Incompatible widget updates.");
    await expect(page.getByText("Valid prefix applied")).toBeVisible();
    await expect(page.getByText("Trailing update must not be applied")).toHaveCount(0);

    await page.reload();
    await expect(error).toHaveCount(0);
    await expect(page.getByText("Model count: 0")).toBeVisible();
    await increment.click();
    await expect(page.getByText("Model count: 1")).toBeVisible();
});
