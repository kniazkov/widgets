import { test, expect } from "@playwright/test";

function captureChannel(page) {
    const state = { channel: null, csrf: null, session: null };
    page.on("request", request => {
        const csrf = request.headers()["x-widgets-csrf"];
        if (csrf) state.csrf = csrf;
        const session = request.headers()["x-widgets-session"];
        if (session) state.session = session;
    });
    page.on("response", async response => {
        if (response.request().postData()?.includes('name="action"\r\n\r\nnew instance\r\n')) {
            const data = await response.json();
            if (data.id) state.channel = data;
        }
    });
    return state;
}

async function open(page) {
    await page.goto("/channel-security");
    await expect(page.getByText("Private counter: 0", { exact: true })).toBeVisible();
}

function headers(state) {
    return {
        "X-Widgets-CSRF": state.csrf,
        "X-Widgets-Session": state.session,
        "X-Widgets-Channel": state.channel.channelToken,
        "X-Widgets-Epoch": state.channel.serverId
    };
}

test("another browser cannot synchronize or kill a page even with its channel secret", async ({
    page,
    browser
}) => {
    const victim = captureChannel(page);
    await open(page);
    await expect.poll(() => victim.channel?.id).toBeTruthy();
    const independent = await browser.newContext();
    try {
        const other = await independent.newPage();
        const attacker = captureChannel(other);
        await other.goto(page.url());
        await expect(other.getByText("Private counter: 0", { exact: true })).toBeVisible();
        await expect.poll(() => attacker.csrf).toBeTruthy();
        for (const action of ["synchronize", "kill", "report error"]) {
            const response = await independent.request.post(new URL("/", page.url()).href, {
                headers: {
                    ...headers(victim),
                    "X-Widgets-CSRF": attacker.csrf,
                    "X-Widgets-Session": attacker.session
                },
                form: { action, client: victim.channel.id, error: "forged error" }
            });
            expect(response.status()).toBe(403);
            expect(response.headers()["cache-control"]).toBe("no-store");
            expect(await response.text()).not.toContain("Private counter");
        }
        await page.getByText("Change private counter", { exact: true }).click();
        await expect(page.getByText("Private counter: 1", { exact: true })).toBeVisible();
        const stored = await page.evaluate(() => ({ ...localStorage, ...sessionStorage }));
        expect(JSON.stringify(stored)).not.toContain(victim.channel.channelToken);
        expect(JSON.stringify(stored)).not.toContain(victim.csrf);
        expect(await page.evaluate(() => document.cookie)).not.toContain("widgets-session");
    } finally {
        await independent.close();
    }
});

test("session revocation rotates credentials across tabs and history cannot restore access", async ({
    page,
    context
}) => {
    const captured = captureChannel(page);
    await open(page);
    const identity = page.getByText(/^Transport session: /);
    const before = await identity.textContent();
    const other = await context.newPage();
    await open(other);
    await expect(other.getByText(before, { exact: true })).toBeVisible();
    await page.getByText("Change private counter", { exact: true }).click();
    await expect(page.getByText("Private counter: 1", { exact: true })).toBeVisible();
    await page.getByText("Next private page", { exact: true }).click();
    await expect(page).toHaveURL(/channel-security-next$/);
    await expect(page.getByText("Private counter: 0", { exact: true })).toBeVisible();
    await page.goBack();
    await expect(page.getByText("Private counter: 1", { exact: true })).toBeVisible();
    const old = {
        csrf: captured.csrf,
        session: captured.session,
        channel: { ...captured.channel }
    };
    await page.getByText("Revoke browser session", { exact: true }).click();
    await expect(identity).not.toHaveText(before);
    await expect(page.getByText("Private counter: 0", { exact: true })).toBeVisible();
    await expect(other.getByText(/^Transport session: /)).not.toHaveText(before);
    const replay = await context.request.post(new URL("/", page.url()).href, {
        headers: headers(old),
        form: { action: "synchronize", client: old.channel.id }
    });
    expect(replay.status()).toBe(403);
    await page.goForward();
    await expect(page.getByText("Private counter: 0", { exact: true })).toBeVisible();
    await expect(page.getByText(/^Transport session: /)).not.toHaveText(before);
    await other.close();
});
