import { test, expect } from "@playwright/test";

test("normal input and keyboard work; authenticated forged events cannot mutate widgets", async ({
    page
}) => {
    await page.goto("/event-validation");
    const output = page.getByText("Accepted clicks: 0", { exact: true });
    await expect(output).toBeVisible();
    await page.locator("input").fill("Проверка ввода");
    await expect(page.getByText("Echo: Проверка ввода", { exact: true })).toBeVisible();
    await page.locator("select").selectOption({ index: 1 });
    await expect(page.getByText("Selected: 1", { exact: true })).toBeVisible();
    const button = page.getByRole("button", { name: "Guarded action", exact: true });
    const pointer = {
        position: Object.fromEntries(
            ["element", "client", "page", "screen"].map(name => [name, { x: 0, y: 0 }])
        ),
        type: "mouse",
        primary: true,
        buttons: 0,
        pressure: 0,
        keys: { ctrl: false, alt: false, shift: false, meta: false }
    };
    // Exercise the actual HTTP boundary without depending on private runtime closures.
    const ids = await Promise.all(
        [output, button, page.locator("select")].map(locator =>
            locator.evaluate(element => element._id)
        )
    );
    const attacks = [
        { widget: ids[0], type: "text input", data: { text: "forged output" } },
        { widget: ids[1], type: "click", data: pointer },
        { widget: ids[2], type: "select", data: { index: 999 } },
        { widget: ids[2], type: "unknown event", data: {} }
    ];
    // Capture the browser's next legitimate ID and replace that event's contents. This keeps
    // the real browser sequence monotonic so normal interaction can continue after rejection.
    let rejection;
    await page.route("**/*", async route => {
        const request = route.request();
        const body = request.postData();
        if (request.method() === "POST" && body?.includes('name="events"')) {
            const match = body.match(/(name="events"\r\n\r\n)([^\r]+)(\r\n)/);
            if (match) {
                const events = JSON.parse(match[2]);
                const attack = attacks.shift();
                if (attack) {
                    events[0] = { id: events[0].id, ...attack };
                    const response = await route.fetch({
                        postData: body.replace(
                            match[0],
                            match[1] + JSON.stringify(events) + match[3]
                        )
                    });
                    rejection = await response.json();
                    await route.fulfill({ response });
                    return;
                }
            }
        }
        await route.continue();
    });
    const reasons = ["unsupportedEvent", "inactiveWidget", "invalidPayload", "unknownEvent"];
    for (const reason of reasons) {
        rejection = null;
        await page.getByText("Enable guarded action", { exact: true }).click();
        await expect.poll(() => rejection?.rejectedEvents?.[0]?.reason).toBe(reason);
    }
    await page.unroute("**/*");
    await expect(output).toBeVisible();
    await page.getByText("Enable guarded action", { exact: true }).click();
    await expect(button).toBeEnabled();
    await button.focus();
    await page.keyboard.press("Enter");
    await expect(page.getByText("Accepted clicks: 1", { exact: true })).toBeVisible();
});
