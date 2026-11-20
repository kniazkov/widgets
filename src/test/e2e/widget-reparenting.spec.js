import { expect, test } from "@playwright/test";

test("live reparent keeps order, DOM identity and a local draft after former-owner disposal", async ({
    page
}) => {
    await page.goto("/widget-reparenting");
    const input = page.getByRole("textbox", { name: "Movable draft" });
    const original = await input.elementHandle();
    const id = await input.evaluate(widget => widget._id);
    await input.evaluate(widget => {
        widget.value = "unsynchronized draft";
        widget.setSelectionRange(2, 7);
    });
    await page.getByRole("button", { name: "Move to destination" }).click();
    await expect(
        input.locator("xpath=../preceding-sibling::*[1]").getByText("Destination marker")
    ).toBeVisible();
    await expect(input).toHaveValue("unsynchronized draft");
    expect(
        await input.evaluate(
            (widget, previous) => widget === previous && widget.selectionStart === 2,
            original
        )
    ).toBe(true);
    expect(
        await page.evaluate(
            identifier =>
                [...document.querySelectorAll("input")].filter(node => node._id === identifier)
                    .length,
            id
        )
    ).toBe(1);
    await page.getByRole("button", { name: "Dispose source" }).click();
    await expect(page.getByText("Source marker")).toHaveCount(0);
    await page.getByRole("button", { name: "Moved action" }).click();
    await expect(page.getByText("Actions: 1")).toBeVisible();
    await expect(input).toHaveValue("unsynchronized draft");
});

test("column moves the existing cell and rejects a cycle without adding a row", async ({
    page
}) => {
    await page.goto("/widget-reparenting");
    const cell = page.getByText("Movable cell").locator("xpath=ancestor::td[1]");
    const original = await cell.elementHandle();
    await page.getByRole("button", { name: "Reject table cycle" }).click();
    await expect(page.getByText("Cycle rejected; rows: 1")).toBeVisible();
    expect(await cell.evaluate(node => node.cellIndex)).toBe(0);

    await page.getByRole("button", { name: "Move table cell" }).click();
    await expect(page.getByText("Moved to row 2, column 2")).toBeVisible();
    expect(await cell.evaluate((node, old) => node === old && node.cellIndex === 1, original)).toBe(
        true
    );
    expect(await cell.evaluate(node => node.parentElement.rowIndex)).toBe(1);
    await page.getByRole("button", { name: "Reject table cycle" }).click();
    await expect(page.getByText("Cycle rejected; rows: 2")).toBeVisible();
});
