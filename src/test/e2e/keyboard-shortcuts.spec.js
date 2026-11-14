import { expect, test } from "@playwright/test";

function keyboardEvents(request) {
    const match = request.postData()?.match(/name="events"\r\n\r\n([^\r]+)\r\n/);
    return request.method() === "POST" && match
        ? JSON.parse(match[1]).filter(event => event.type === "keyboard")
        : [];
}

test("Enter uses fresh unblurred demo input exactly once; click shares validation", async ({
    page
}) => {
    const packets = [];
    page.on("request", request => packets.push(...keyboardEvents(request)));
    await page.goto("/all-widgets");
    const user = page.getByRole("textbox", { name: "Demo user", exact: true });
    const code = page.getByLabel("Demo code", { exact: true });
    await user.pressSequentially("demo");
    await code.pressSequentially("bad");
    await code.press("Enter");
    await expect(page.getByText("Attempt 1: check the test data", { exact: true })).toBeVisible();
    await expect(code).toBeFocused();
    await expect(code).toHaveValue("bad");

    await code.fill("");
    await code.pressSequentially("blue");
    await page.keyboard.down("NumpadEnter");
    await page.keyboard.down("NumpadEnter");
    await page.keyboard.up("NumpadEnter");
    await expect(page.getByText("Attempt 2: demo accepted", { exact: true })).toBeVisible();
    await code.press("Enter");
    await expect(page.getByText("Attempt 3: demo accepted", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Sign in demo", exact: true }).click();
    await expect(page.getByText("Attempt 4: demo accepted", { exact: true })).toBeVisible();
    expect(packets).toHaveLength(3);
    for (const { data } of packets) {
        expect(data).toEqual({
            key: "Enter",
            phase: "DOWN",
            ctrl: false,
            alt: false,
            shift: false,
            meta: false
        });
    }
});

test("a slow or lost Enter acknowledgement does not retry the demo action", async ({ page }) => {
    await page.goto("/all-widgets");
    const user = page.getByRole("textbox", { name: "Demo user", exact: true });
    const code = page.getByLabel("Demo code", { exact: true });
    await user.fill("demo");
    await code.fill("blue");
    const ids = [];
    let delivered;
    const firstRan = new Promise(resolve => {
        delivered = resolve;
    });
    let release;
    const delay = new Promise(resolve => {
        release = resolve;
    });
    await page.route("**/*", async route => {
        const incoming = keyboardEvents(route.request());
        if (!incoming.length) return route.continue();
        ids.push(...incoming.map(event => event.id));
        const response = await route.fetch();
        if (ids.length === 1) {
            delivered();
            await delay;
            return route.abort("failed");
        }
        return route.fulfill({ response });
    });
    try {
        await code.press("Enter");
        await firstRan;
        await code.press("Enter");
        await expect(code).toBeFocused();
    } finally {
        release();
    }
    await expect(page.getByText("Attempt 2: demo accepted", { exact: true })).toBeVisible();
    expect(ids).toHaveLength(3);
    expect(ids[1]).toBe(ids[0]);
    expect(ids[2]).not.toBe(ids[0]);
});

test("normal multiline, suggestion, disabled and cached-page behavior is preserved", async ({
    page
}) => {
    await page.goto("/keyboard-shortcuts");
    const area = page.getByLabel("Multiline draft", { exact: true });
    const suggestion = page.getByRole("combobox", { name: "Keyboard suggestion", exact: true });
    const locked = page.getByRole("textbox", { name: "Read-only editor" });
    const disabled = page.getByRole("textbox", { name: "Disabled editor" });
    await expect(disabled).toBeDisabled();
    await expect(locked).not.toBeEditable();
    await locked.press("Enter");
    await area.pressSequentially("draft");
    await area.press("Enter");
    await expect(area).toHaveValue("draft\n");
    await area.press("Tab");
    await expect(suggestion).toBeFocused();
    await suggestion.fill("Sil");
    await suggestion.press("ArrowDown");
    await suggestion.press("Enter");
    await expect(suggestion).toHaveValue("Silver");
    await expect(page.getByText("Keyboard actions: 0", { exact: true })).toBeVisible();
    await area.press("Control+Enter");
    await expect(area).toHaveValue("draft\n");
    await expect(page.getByText("Keyboard actions: 1", { exact: true })).toBeVisible();

    await page.getByRole("link", { name: "Open other keyboard page" }).click();
    await expect(page).not.toHaveURL(/keyboard-shortcuts$/);
    await page.goBack();
    await expect(area).toHaveValue("draft\n");
    await area.press("Control+Enter");
    await expect(page.getByText("Keyboard actions: 2", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Dispose suggestion" }).click();
    await expect(suggestion).toHaveCount(0);
});
