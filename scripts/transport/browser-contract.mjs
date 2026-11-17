import assert from "node:assert/strict";
import { setTimeout as delay } from "node:timers/promises";

export async function install(page, mode, root) {
    await page.addScriptTag({ path: root + "/scripts/transport/socket-browser.js" });
    await page.evaluate(async mode => {
        window.probeMode = mode;
        window.probeReconnect = async () => {
            window.probeTransport?.close();
            window.probeTransport =
                mode === "http" ? createHttpTransport() : createPrototypeSocketTransport();
            await window.probeTransport.connect();
        };
        await window.probeReconnect();
        const original = sendRequest;
        sendRequest = (query, callback, method, files, channel) =>
            window.probeFreeze
                ? () => {}
                : files?.length
                  ? original(query, callback, method, files, channel)
                  : window.probeTransport.send({ query, method, files, channel }, callback);
        window.probeCall = (query, c) =>
            new Promise(resolve => {
                window.probeTransport.send(
                    {
                        query,
                        channel: c
                            ? {
                                  token: c.channelToken,
                                  epoch: c.serverId,
                                  updateEpoch: c.updateEpoch
                              }
                            : undefined
                    },
                    data => resolve(data === null ? null : JSON.parse(data))
                );
            });
        window.probeApply = (c, response) => {
            for (const u of response.updates || []) {
                if (
                    u.action === "create widget" &&
                    u.type === "button" &&
                    !c.buttons.includes(u.widget)
                )
                    c.buttons.push(u.widget);
                if (u.action === "set text" && /^Count: \d+$/.test(u.text))
                    c.count = Number(u.text.slice(7));
            }
            if (response.updateThrough !== undefined) c.lastUpdate = response.updateThrough;
        };
        window.probeSync = async (c, events = [], checkpoint = c.lastUpdate) => {
            const response = await window.probeCall(
                { action: "synchronize", client: c.id, events, lastUpdate: "#" + checkpoint },
                c
            );
            if (response) window.probeApply(c, response);
            return response;
        };
        window.probeNew = async tabId => {
            const c = await window.probeCall({
                action: "new instance",
                address: "/",
                browserId: "12345678-1234-4234-8234-123456789abc",
                mobile: false,
                clientBuild: widgetsClientBuild,
                updateVersion: "2",
                updateCapability: "sequenced-updates",
                ...(tabId ? { tabId } : {})
            });
            c.lastUpdate = 0;
            c.event = 0;
            c.buttons = [];
            let reply;
            do {
                reply = await window.probeSync(c);
            } while (reply.moreUpdates);
            await window.probeSync(c);
            return c;
        };
        window.probeEvent = (c, index = 0) => ({
            id: "#" + ++c.event,
            widget: c.buttons[index],
            type: "click",
            data: {
                type: "mouse",
                primary: true,
                buttons: 0,
                pressure: 0,
                keys: { ctrl: false, alt: false, shift: false, meta: false },
                position: Object.fromEntries(
                    ["element", "client", "page", "screen"].map(k => [k, { x: 10, y: 20 }])
                )
            }
        });
    }, mode);
}

export async function contract(page, browser, origin, mode, root, restart) {
    const cases = [];
    await page.evaluate(async () => {
        window.probeClient = await window.probeNew();
        window.probeCommand = window.probeEvent(window.probeClient);
        await window.probeSync(window.probeClient, [window.probeCommand]);
        await window.probeSync(window.probeClient, [window.probeCommand]);
    });
    assert.equal(await page.evaluate(() => window.probeClient.count), 1);
    cases.push("same event ID executes once");
    const denied = await page.evaluate(async () => {
        const c = { ...window.probeClient, channelToken: "incorrect-secret" };
        return window.probeCall({ action: "synchronize", client: c.id }, c);
    });
    assert.equal(denied.accessDenied, true);
    const epoch = await page.evaluate(() =>
        window.probeCall(
            { action: "synchronize", client: window.probeClient.id, lastUpdate: "#0", events: [] },
            { ...window.probeClient, updateEpoch: "old-generation" }
        )
    );
    assert.equal(epoch.protocolMismatch, true);
    cases.push("channel secret and update epoch enforced");
    const foreign = await browser.newContext();
    try {
        const attacker = await foreign.newPage();
        await attacker.goto(origin);
        await attacker.getByRole("button", { name: "Increment", exact: true }).waitFor();
        await install(attacker, mode, root);
        const victim = await page.evaluate(() => window.probeClient);
        const reply = await attacker.evaluate(
            c => window.probeCall({ action: "synchronize", client: c.id }, c),
            victim
        );
        assert.equal(reply.accessDenied, true);
        cases.push("another browser cannot use stolen channel credentials");
    } finally {
        await foreign.close();
    }
    // Drop a reply after the Java command commits; retry exactly the same event ID.
    const uncertain = await page.evaluate(async () => {
        window.probeDelayed = window.probeEvent(window.probeClient, 1);
        const pending = window.probeSync(window.probeClient, [window.probeDelayed]);
        setTimeout(() => window.probeTransport.close(), 150);
        return pending;
    });
    assert.equal(uncertain, null);
    await delay(800);
    await page.evaluate(async () => {
        await window.probeReconnect();
        await window.probeSync(window.probeClient, [window.probeDelayed]);
    });
    assert.equal(await page.evaluate(() => window.probeClient.count), 2);
    cases.push("disconnect/reconnect replays unacknowledged command without duplication");
    await page.evaluate(async () => {
        window.probeOldCheckpoint = window.probeClient.lastUpdate;
        await window.probeSync(window.probeClient, [window.probeEvent(window.probeClient)]);
    });
    await delay(5500);
    const expired = await page.evaluate(() =>
        window.probeSync(window.probeClient, [], window.probeOldCheckpoint)
    );
    assert.equal(expired.updateResyncRequired, true);
    await page.evaluate(async () => {
        window.probeClient = await window.probeNew(window.probeClient.tabId);
    });
    assert.equal(await page.evaluate(() => window.probeClient.count), 3);
    cases.push("slow client resync preserves committed business state");
    const oversized = await page.evaluate(() =>
        window.probeSync(
            window.probeClient,
            Array.from({ length: 65 }, () => window.probeEvent(window.probeClient))
        )
    );
    assert.equal(oversized.eventBackpressure, true);
    await page.evaluate(async () => {
        window.probeClient = await window.probeNew(window.probeClient.tabId);
    });
    assert.equal(await page.evaluate(() => window.probeClient.count), 3);
    cases.push("oversized event batch is rejected without side effects");
    await page.evaluate(() => {
        window.probeFreeze = true;
        return window.probeSync(window.probeClient, [window.probeEvent(window.probeClient, 2)]);
    });
    const revoked = await page.evaluate(() => window.probeSync(window.probeClient));
    assert.equal(revoked.sessionExpired, true);
    cases.push("logout revokes the existing connection's credentials");
    // A fresh browser document obtains new auth; retain it while restarting Java.
    await page.goto(origin);
    await page.getByRole("button", { name: "Increment", exact: true }).waitFor();
    await install(page, mode, root);
    await page.evaluate(async () => {
        window.probeClient = await window.probeNew();
    });
    await page.evaluate(() => {
        window.probeFreeze = true;
    });
    await delay(100);
    await restart();
    const stale = await page.evaluate(() => window.probeSync(window.probeClient));
    assert(stale.sessionExpired || stale.accessDenied || stale.protocolMismatch);
    assert(!stale.updates?.length);
    cases.push("server restart rejects old session/epoch without private updates");
    return cases;
}
