import { expect, test } from "@playwright/test";

function demo(page) {
    const table = page.getByRole("table", { name: "Sample order archive" });
    const view = page.locator(".widgets-data-table").filter({ has: table });
    return {
        table,
        view,
        filter: view.getByRole("searchbox", { name: "Order filter" }),
        rows: table.getByRole("row"),
        status: view.getByRole("status"),
        next: view.getByRole("button", { name: "Next" }),
        previous: view.getByRole("button", { name: "Previous" }),
        first: view.getByRole("checkbox", { name: "Select: Sample order #1", exact: true })
    };
}

test("bounded pages, column semantics, key selection and filtered empty archive", async ({
    page
}) => {
    await page.goto("/all-widgets");
    await page.getByRole("heading", { name: "Data table" }).scrollIntoViewIfNeeded();
    const { table, view, filter, rows, status, next, previous, first } = demo(page);
    await expect(rows).toHaveCount(6);
    await expect(view.getByText("Page 1/25 · 125")).toBeVisible();
    const heading = table.getByRole("columnheader", { name: /^Order name/ });
    await expect(heading).toHaveAttribute("scope", "col");
    await first.focus();
    await first.press("Space");
    await expect(first).toBeChecked();
    await expect(first).toBeFocused();
    await next.click();
    await expect(view.getByText("Page 2/25 · 125")).toBeVisible();
    await expect(table.getByText("Sample order #6", { exact: true })).toBeVisible();
    await previous.click();
    await expect(first).toBeChecked();

    await heading.getByRole("button").press("Enter");
    await expect(heading).toHaveAttribute("aria-sort", "ascending");
    await expect(first).toBeChecked();
    await heading.getByRole("button").press("Enter");
    await expect(heading).toHaveAttribute("aria-sort", "descending");

    await filter.fill("active");
    await expect(status).toHaveText("No matching orders.");
    await expect(rows).toHaveCount(1);
    await filter.fill("");
    await expect(rows).toHaveCount(6);
    await expect(view.getByText("Page 1/25 · 125")).toBeVisible();
});

test("superseded filters, one result, retry and disabled state", async ({ page }) => {
    await page.goto("/all-widgets");
    await page.getByRole("heading", { name: "Data table" }).scrollIntoViewIfNeeded();
    const { table, view, filter, rows, status } = demo(page);
    await expect(rows).toHaveCount(6);
    await filter.fill("slow");
    await expect(table).toHaveAttribute("aria-busy", "true");
    await filter.fill("fast");
    await expect(table.getByText("Sample order #2", { exact: true })).toBeVisible();
    await expect(rows).toHaveCount(2);
    await expect(filter).toBeFocused();
    await expect(view.getByText("Page 1/1 · 1")).toBeVisible();
    await expect(table.getByText("Sample order #1", { exact: true })).toHaveCount(0);
    await filter.fill("error");
    await expect(status).toHaveText("Archive temporarily unavailable.");
    await expect(view.getByRole("button", { name: "Retry" })).toBeVisible();
    await filter.fill("");
    await expect(rows).toHaveCount(6);
    await page.getByRole("button", { name: "Enable / disable table" }).click();
    await expect(filter).toBeDisabled();
    await expect(view.getByRole("checkbox").first()).toBeDisabled();
    await page.getByRole("button", { name: "Enable / disable table" }).click();
    await expect(filter).toBeEnabled();
});

test("single-row replacement and authoritative deletion retain keyed selection", async ({
    page
}) => {
    await page.goto("/all-widgets");
    await page.getByRole("heading", { name: "Data table" }).scrollIntoViewIfNeeded();
    const { table, first, view } = demo(page);
    await first.check();
    await expect(first).toBeChecked();
    const original = await first.elementHandle();
    await page.getByRole("button", { name: "Update first record" }).click();
    await first.focus();
    await expect(table.getByText("Updated")).toBeVisible();
    await expect(first).toBeFocused();
    expect(await original.evaluate(node => node.isConnected && node.checked)).toBe(true);
    await original.dispose();
    await page.getByRole("button", { name: "Delete first order" }).click();
    await expect(first).toHaveCount(0);
    await expect(view.getByText("Page 1/25 · 124")).toBeVisible();
    await expect(view.getByRole("checkbox").first()).not.toBeChecked();
});

test.describe("390 px touch table", () => {
    test.use({ viewport: { width: 390, height: 760 }, hasTouch: true });
    test("only the table scrolls horizontally while role names and touch stay usable", async ({
        page
    }) => {
        await page.goto("/all-widgets");
        await page.getByRole("heading", { name: "Data table" }).scrollIntoViewIfNeeded();
        const { table, view, filter, first } = demo(page);
        const region = view.getByRole("region", { name: "Sample order archive" });
        await expect(first).toBeVisible();
        expect(await region.evaluate(element => element.scrollWidth > element.clientWidth)).toBe(
            true
        );
        expect(await view.evaluate(element => element.scrollWidth <= element.clientWidth + 1)).toBe(
            true
        );
        await first.tap();
        await expect(first).toBeChecked();
        await filter.tap();
        await filter.fill("fast");
        await expect(table.getByText("Sample order #2", { exact: true })).toBeVisible();
        await expect(filter).toBeFocused();
    });
});
