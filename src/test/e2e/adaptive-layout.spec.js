import { expect, test } from "@playwright/test";

function field(page, name) {
    return page.getByRole("textbox", { name });
}

function form(page) {
    return page.getByRole("form", { name: "Adaptive sample request" });
}

const title = "Full sample item description for testing label wrapping";
const longWord = "LongWordWithoutSpacesToTestGridWrapping";

async function placement(first, second) {
    const a = await first.boundingBox();
    const b = await second.boundingBox();
    return { dx: Math.round(b.x - a.x), dy: Math.round(b.y - a.y) };
}

test("stack direction and nested grids follow their own width, not the device", async ({
    page
}) => {
    await page.setViewportSize({ width: 1280, height: 780 });
    await page.goto("/all-widgets");
    const alpha = page.getByText("Alpha · Stack", { exact: true });
    const beta = page.getByText("Beta · Stack", { exact: true });
    const wideA = page.getByText("Wide grid A", { exact: true });
    const wideB = page.getByText("Wide grid B", { exact: true });
    const innerA = page.getByText("Narrow grid A", { exact: true });
    const innerB = page.getByText("Narrow grid B", { exact: true });
    await expect(beta).toBeVisible();
    expect(await placement(alpha, beta)).toMatchObject({
        dx: expect.any(Number),
        dy: 0
    });
    expect((await placement(alpha, beta)).dx).toBeGreaterThan(0);
    expect((await placement(wideA, wideB)).dx).toBeGreaterThan(0);
    expect((await placement(innerA, innerB)).dy).toBeGreaterThan(0);
    await page.getByRole("button", { name: "Switch Stack direction" }).click();
    await expect.poll(async () => (await placement(alpha, beta)).dy).toBeGreaterThan(0);
    await page.setViewportSize({ width: 320, height: 780 });
    await expect.poll(async () => (await placement(wideA, wideB)).dy).toBeGreaterThan(0);
    expect((await placement(innerA, innerB)).dy).toBeGreaterThan(0);
    expect(
        await page.getByText(longWord, { exact: true }).evaluate(node => {
            const range = document.createRange();
            range.selectNodeContents(node);
            const grid = node.closest(".widgets-responsive-grid");
            return range.getClientRects().length > 1 && grid.scrollWidth <= grid.clientWidth + 1;
        })
    ).toBe(true);
});

test("form columns become rows on resize and retain input DOM identity, selection and focus", async ({
    page
}) => {
    await page.setViewportSize({ width: 1280, height: 780 });
    await page.goto("/all-widgets");
    const group = form(page);
    const layout = group.locator(".widgets-form-layout");
    const editor = field(page, title);
    const label = group.locator("label").filter({ hasText: title });
    await expect(layout).toHaveAttribute("data-label-columns", "");
    expect((await placement(label, editor)).dx).toBeGreaterThan(100);
    await label.click();
    await expect(editor).toBeFocused();
    await editor.fill("Paper");
    await editor.evaluate(node => {
        node.setSelectionRange(2, 4);
        node.dataset.resizeProbe = "original";
    });
    await page.setViewportSize({ width: 390, height: 780 });
    await expect(layout).not.toHaveAttribute("data-label-columns", "");
    expect((await placement(label, editor)).dy).toBeGreaterThan(10);
    await expect(editor).toBeFocused();
    await expect(editor).toHaveValue("Paper");
    await expect(editor).toHaveAttribute("data-resize-probe", "original");
    expect(await editor.evaluate(node => [node.selectionStart, node.selectionEnd])).toEqual([2, 4]);
    await page.setViewportSize({ width: 1280, height: 780 });
    await expect(layout).toHaveAttribute("data-label-columns", "");
    await expect(editor).toBeFocused();
    await page.getByRole("button", { name: "Switch form breakpoint" }).click();
    await expect(layout).not.toHaveAttribute("data-label-columns", "");
    await expect(editor).toHaveAttribute("data-resize-probe", "original");
    await expect(editor).toHaveValue("Paper");
});

test("200% layout zoom stacks labels and keeps normal Tab / Enter validation", async ({ page }) => {
    await page.setViewportSize({ width: 1024, height: 780 });
    await page.goto("/all-widgets");
    const group = form(page);
    const layout = group.locator(".widgets-form-layout");
    const editor = field(page, title);
    const note = field(page, "Additional note");
    await expect(layout).toHaveAttribute("data-label-columns", "");
    await page.locator("body").evaluate(node => (node.style.zoom = "2"));
    await expect(layout).not.toHaveAttribute("data-label-columns", "");
    await editor.focus();
    await editor.press("Tab");
    await expect(note).toBeFocused();
    await note.press("Enter");
    await expect(group.getByRole("status")).toHaveText("Complete the highlighted field.");
    await expect(editor).toBeFocused();
    await expect(editor).toHaveAccessibleDescription(/Enter a description/);
});

test.describe("adaptive controls on a touch viewport", () => {
    test.use({ viewport: { width: 390, height: 780 }, hasTouch: true });
    test("taps a stacked form label and existing submit control", async ({ page }) => {
        await page.goto("/all-widgets");
        const group = form(page);
        const editor = field(page, title);
        await expect(group.locator(".widgets-form-layout")).not.toHaveAttribute(
            "data-label-columns",
            ""
        );
        await group.locator("label").filter({ hasText: title }).tap();
        await expect(editor).toBeFocused();
        await editor.fill("Paper");
        await group.getByRole("button", { name: "Validate layout" }).tap();
        await expect(group.getByRole("status")).toHaveText("Layout validated.");
    });
});

for (const mode of ["during", "before"]) {
    test(`controlled table completion ${mode} pointer press`, async ({ page }, info) => {
        let release;
        const gate = new Promise(resolve => {
            release = resolve;
        });
        let held;
        const ready = new Promise(resolve => {
            held = resolve;
        });
        await page.route("**/*", async route => {
            if (route.request().method() !== "POST") return route.continue();
            const response = await route.fetch();
            const data = await response.json();
            if (
                data.updates?.some(
                    u =>
                        u.action === "set data table" &&
                        u.snapshot?.caption === "Sample order archive" &&
                        u.snapshot.state === "ready"
                )
            ) {
                held();
                await gate;
            }
            await route.fulfill({ response });
        });
        await page.setViewportSize({ width: 1280, height: 780 });
        await page.goto("/all-widgets");
        await page
            .getByRole("region", { name: "Sample order archive", exact: true })
            .scrollIntoViewIfNeeded();
        await ready;
        const button = page.getByRole("button", { name: "Switch form breakpoint", exact: true });
        const layout = form(page).locator(".widgets-form-layout");
        await field(page, title).fill("Paper");
        await button.scrollIntoViewIfNeeded();
        await page.evaluate(() => {
            window.probe = [];
            for (const type of ["mousedown", "mouseup", "click", "blur"])
                document.addEventListener(
                    type,
                    e =>
                        window.probe.push({
                            type,
                            tag: e.target.tagName,
                            text: e.target.textContent?.slice(0, 70),
                            x: e.clientX,
                            y: e.clientY
                        }),
                    true
                );
        });
        const before = await button.boundingBox();
        const x = before.x + before.width / 2,
            y = before.y + before.height / 2;
        if (mode === "during") {
            await page.mouse.move(x, y);
            await page.mouse.down();
        }
        release();
        await expect(
            page.getByRole("cell", { name: "Sample order #1", exact: true })
        ).toBeAttached();
        const after = await button.boundingBox();
        if (mode === "during") await page.mouse.up();
        else await button.click();
        const events = await page.evaluate(() => window.probe);
        console.log(JSON.stringify({ mode, before, after, events }));
        await info.attach("controlled-evidence", {
            body: JSON.stringify({ mode, before, after, events }, null, 2),
            contentType: "application/json"
        });
        expect(Math.abs(after.y - before.y)).toBeLessThanOrEqual(1);
        expect(
            events.filter(e => e.type === "click" && e.text === "Switch form breakpoint")
        ).toHaveLength(1);
        await expect(layout).not.toHaveAttribute("data-label-columns", "");
    });
}
