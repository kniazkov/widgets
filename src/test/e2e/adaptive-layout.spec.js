import { expect, test } from "@playwright/test";

// Temporary diagnostic branch: preserve successful runs too for comparison with failures.
test.use({ trace: "on" });
const diagnostics = new WeakMap();
test.beforeEach(async ({ page }) => {
    const data = { console: [], network: [], checkpoints: [] };
    diagnostics.set(page, data);
    page.on("console", message =>
        data.console.push({ type: message.type(), text: message.text() })
    );
    page.on("pageerror", error => data.console.push({ type: "pageerror", text: String(error) }));
    page.on("request", request => {
        if (request.method() === "POST")
            data.network.push({
                phase: "request",
                time: Date.now(),
                url: request.url(),
                body: request.postData()
            });
    });
    page.on("response", async response => {
        if (response.request().method() !== "POST") return;
        try {
            data.network.push({
                phase: "response",
                time: Date.now(),
                url: response.url(),
                status: response.status(),
                body: await response.text()
            });
        } catch (error) {
            data.network.push({ phase: "response-error", error: String(error) });
        }
    });
    await page.addInitScript(() => {
        window.__layoutProbe = [];
        for (const type of [
            "pointerdown",
            "mousedown",
            "pointerup",
            "mouseup",
            "click",
            "focusin",
            "focusout",
            "resize",
            "scroll"
        ]) {
            window.addEventListener(
                type,
                event => {
                    const target = event.target;
                    window.__layoutProbe.push({
                        layouts: [...document.querySelectorAll(".widgets-form-layout")].map(
                            node => ({
                                id: node._id,
                                breakpoint: node._formLayoutBreakpoint,
                                columns: node.hasAttribute("data-label-columns")
                            })
                        ),
                        type,
                        time: performance.now(),
                        tag: target?.tagName,
                        id: target?._id,
                        text: target?.textContent?.slice(0, 120),
                        x: event.clientX,
                        y: event.clientY,
                        scrollY,
                        width: innerWidth,
                        height: innerHeight
                    });
                },
                true
            );
        }
    });
});
async function checkpoint(page, phase) {
    const state = await page.evaluate(() => ({
        viewport: { width: innerWidth, height: innerHeight, scrollY },
        focus: { tag: document.activeElement?.tagName, id: document.activeElement?._id },
        layouts: [...document.querySelectorAll(".widgets-form-layout")].map(node => ({
            id: node._id,
            breakpoint: node._formLayoutBreakpoint,
            rect: node.getBoundingClientRect().toJSON(),
            columns: node.hasAttribute("data-label-columns"),
            paused: node._treePaused
        })),
        events: window.__layoutProbe,
        clientError: document.querySelector("#client-error-overlay")?.textContent
    }));
    diagnostics.get(page).checkpoints.push({ phase, time: Date.now(), state });
}
test.afterEach(async ({ page }, info) => {
    try {
        await checkpoint(page, "finished");
    } catch (error) {
        diagnostics.get(page).console.push({ error: String(error) });
    }
    await info.attach("layout-diagnostic", {
        body: JSON.stringify(diagnostics.get(page), null, 2),
        contentType: "application/json"
    });
    await info.attach("final-screen", { body: await page.screenshot(), contentType: "image/png" });
});

function field(page, name) {
    return page.getByRole("textbox", { name });
}

function form(page) {
    return page.getByRole("form", { name: "Адаптивная учебная заявка" });
}

const title = "Полное описание учебного предмета для проверки переноса подписи";
const longWord = "ДлинноесловобезпробеловДляПроверкиПереносаСетки";

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
    const alpha = page.getByText("Альфа · Stack", { exact: true });
    const beta = page.getByText("Бета · Stack", { exact: true });
    const wideA = page.getByText("Широкая сетка А", { exact: true });
    const wideB = page.getByText("Широкая сетка Б", { exact: true });
    const innerA = page.getByText("Узкая сетка А", { exact: true });
    const innerB = page.getByText("Узкая сетка Б", { exact: true });
    await expect(beta).toBeVisible();
    expect(await placement(alpha, beta)).toMatchObject({
        dx: expect.any(Number),
        dy: 0
    });
    expect((await placement(alpha, beta)).dx).toBeGreaterThan(0);
    expect((await placement(wideA, wideB)).dx).toBeGreaterThan(0);
    expect((await placement(innerA, innerB)).dy).toBeGreaterThan(0);
    await page.getByRole("button", { name: "Сменить направление Stack" }).click();
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
    await editor.fill("Бумага");
    await editor.evaluate(node => {
        node.setSelectionRange(2, 4);
        node.dataset.resizeProbe = "original";
    });
    await page.setViewportSize({ width: 390, height: 780 });
    await expect(layout).not.toHaveAttribute("data-label-columns", "");
    expect((await placement(label, editor)).dy).toBeGreaterThan(10);
    await expect(editor).toBeFocused();
    await expect(editor).toHaveValue("Бумага");
    await expect(editor).toHaveAttribute("data-resize-probe", "original");
    expect(await editor.evaluate(node => [node.selectionStart, node.selectionEnd])).toEqual([2, 4]);
    await page.setViewportSize({ width: 1280, height: 780 });
    await expect(layout).toHaveAttribute("data-label-columns", "");
    await expect(editor).toBeFocused();
    await page.getByRole("button", { name: "Сменить порог формы" }).click();
    await expect(layout).not.toHaveAttribute("data-label-columns", "");
    await expect(editor).toHaveAttribute("data-resize-probe", "original");
    await expect(editor).toHaveValue("Бумага");
});

test("200% layout zoom stacks labels and keeps normal Tab / Enter validation", async ({ page }) => {
    await page.setViewportSize({ width: 1024, height: 780 });
    await page.goto("/all-widgets");
    const group = form(page);
    const layout = group.locator(".widgets-form-layout");
    const editor = field(page, title);
    const note = field(page, "Дополнительный комментарий");
    await expect(layout).toHaveAttribute("data-label-columns", "");
    await page.locator("body").evaluate(node => (node.style.zoom = "2"));
    await expect(layout).not.toHaveAttribute("data-label-columns", "");
    await editor.focus();
    await editor.press("Tab");
    await expect(note).toBeFocused();
    await note.press("Enter");
    await expect(group.getByRole("status")).toHaveText("Заполните выделенное поле.");
    await expect(editor).toBeFocused();
    await expect(editor).toHaveAccessibleDescription(/Заполните описание/);
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
        await editor.fill("Бумага");
        await group.getByRole("button", { name: "Проверить макет" }).tap();
        await expect(group.getByRole("status")).toHaveText("Макет проверен.");
    });
});
