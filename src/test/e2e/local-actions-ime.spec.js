import { expect, test } from "@playwright/test";

const editorOf = page => page.getByRole("textbox", { name: "IME editor" });

async function delayedTransport(page) {
    await page.addInitScript(() => {
        const send = XMLHttpRequest.prototype.send;
        window.pendingSyncs = [];
        window.holdSyncs = false;
        window.releaseSyncs = () => {
            window.holdSyncs = false;
            for (const fn of window.pendingSyncs.splice(0)) fn();
        };
        XMLHttpRequest.prototype.send = function (body) {
            if (
                window.holdSyncs &&
                body instanceof FormData &&
                body.get("action") === "synchronize"
            ) {
                window.pendingSyncs.push(() => send.call(this, body));
                return;
            }
            return send.call(this, body);
        };
    });
}
async function hold(page) {
    await Promise.all(
        page.frames().map(f =>
            f.evaluate(() => {
                window.holdSyncs = true;
            })
        )
    );
}
async function release(page) {
    await Promise.all(page.frames().map(f => f.evaluate(() => window.releaseSyncs?.())));
}

test("focus and selection do not wait for RTT or roll back on business rejection", async ({
    page
}) => {
    await delayedTransport(page);
    await page.goto("/local-actions");
    const editor = editorOf(page);
    await expect(editor).toHaveValue("abcdef");
    await hold(page);
    await page
        .getByRole("button", { name: "Local focus and rejected command", exact: true })
        .click();
    await expect(editor).toBeFocused();
    await expect(page.getByText("No business command", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Select characters 1 to 4", exact: true }).click();
    expect(await editor.evaluate(e => [e.selectionStart, e.selectionEnd])).toEqual([1, 4]);
    await release(page);
    await expect(page.getByText("Business command rejected", { exact: true })).toBeVisible();
    await expect(editor).toBeFocused();
});

test("rapid input under delayed responses preserves caret and flushes before Enter", async ({
    page
}) => {
    await delayedTransport(page);
    await page.goto("/local-actions");
    const editor = editorOf(page);
    await expect(editor).toHaveValue("abcdef");
    await editor.fill("a");
    await hold(page);
    await editor.fill("ab");
    await editor.press("ArrowLeft");
    await editor.press("x");
    await expect(editor).toHaveValue("axb");
    expect(await editor.evaluate(e => e.selectionStart)).toBe(2);
    await editor.press("Enter");
    await release(page);
    await expect(page.getByText("Submitted: AXB", { exact: true })).toBeVisible();
    await expect(page.getByText("Committed edits: 1", { exact: true })).toBeVisible();
    await expect(editor).toHaveValue("axb");
    expect(await editor.evaluate(e => e.selectionStart)).toBe(2);
    await editor.press("Tab");
    await expect(editor).toHaveValue("AXB");
});

test("composition sequence submits only committed text and ignores duplicate final input", async ({
    page
}) => {
    await page.goto("/local-actions");
    const editor = editorOf(page);
    await expect(editor).toHaveValue("abcdef");
    await editor.focus();
    await editor.evaluate(e => {
        e.dispatchEvent(new CompositionEvent("compositionstart", { bubbles: true }));
        e.value = "ni";
        e.dispatchEvent(new InputEvent("input", { bubbles: true, isComposing: true }));
        e.dispatchEvent(new CompositionEvent("compositionupdate", { bubbles: true, data: "你" }));
        e.value = "你";
        e.dispatchEvent(new InputEvent("input", { bubbles: true, isComposing: true }));
    });
    await expect(page.getByText("Committed edits: 0", { exact: true })).toBeVisible();
    await editor.evaluate(e => {
        e.dispatchEvent(new CompositionEvent("compositionend", { bubbles: true, data: "你" }));
        e.dispatchEvent(new InputEvent("input", { bubbles: true, isComposing: false }));
    });
    await expect(page.getByText("Committed edits: 1", { exact: true })).toBeVisible();
    await expect(editor).toHaveValue("你");
    await editor.press("Tab");
    await expect(page.getByText("Committed edits: 1", { exact: true })).toBeVisible();
});

test("new tab opens within the original click and has no opener", async ({ page }) => {
    await page.goto("/local-actions");
    await expect(editorOf(page)).toHaveValue("abcdef");
    const popup = page.context().waitForEvent("page");
    await page.getByRole("button", { name: "Open example in new tab", exact: true }).click();
    const opened = await popup;
    await expect(opened).toHaveURL(/\/local-actions$/);
    expect(await opened.evaluate(() => window.opener)).toBeNull();
    await opened.close();
});
