import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const source = fs.readFileSync(
    new URL("../../main/html/scripts/client.js", import.meta.url),
    "utf8"
);

const handlerNames = [
    "createWidget",
    "reset",
    "goToPage",
    "subscribeToEvent",
    "setChildWidget",
    "appendChildWidget",
    "removeChildWidget",
    "setValidFlag",
    "setDisabledFlag",
    "setHiddenFlag",
    "setText",
    "setHref",
    "setColor",
    "setBgColor",
    "setOpacity",
    "setWidth",
    "setHeight",
    "setMargin",
    "setPadding",
    "setFontFace",
    "setFontSize",
    "setFontWeight",
    "setItalic",
    "setTextDecoration",
    "setBorderColor",
    "setBorderStyle",
    "setBorderWidth",
    "setBorderRadius",
    "setBoxShadow",
    "setOutline",
    "setCursor",
    "setTransition",
    "setBoxSizing",
    "setOverflow",
    "setSource",
    "setSelectedSource",
    "setUnselectedSource",
    "setHorzAlignment",
    "setVertAlignment",
    "setCellSpacing",
    "setCheckedFlag",
    "setMultipleInput",
    "setAcceptedFiles"
];

let dom;

afterEach(() => {
    dom?.window.close();
});

function createHarness() {
    dom = new JSDOM("<!doctype html><body><main>Application</main></body>", {
        runScripts: "outside-only",
        url: "http://localhost/example?item=42"
    });
    const handlers = handlerNames.map(name => `function ${name}() { return true; }`).join("\n");
    dom.window.eval(`${handlers}\n${source}\n
        window.__reloadCount = 0;
        reloadCurrentPage = function () {
            window.__reloadCount++;
        };
        window.__clientHarness = {
            fail: recordRequestFailure,
            succeed: recordRequestSuccess,
            reportClientError: responseHasClientError,
            setServerId: function (value) { serverId = value; },
            serverStateIsCurrent: serverStateIsCurrent
        };
    `);
    return dom.window.__clientHarness;
}

describe("connection recovery", () => {
    it("blocks the page after three consecutive failures and unblocks on recovery", () => {
        const harness = createHarness();

        harness.fail();
        harness.fail();
        expect(dom.window.document.getElementById("connection-terminated-overlay")).toBeNull();

        harness.fail();
        const overlay = dom.window.document.getElementById("connection-terminated-overlay");
        expect(overlay?.textContent).toBe("Connection Terminated");

        harness.succeed();
        expect(dom.window.document.getElementById("connection-terminated-overlay")).toBeNull();
    });

    it("shows a permanent client error reported by the server", () => {
        const harness = createHarness();

        expect(harness.reportClientError({ clientError: true })).toBe(true);
        const overlay = dom.window.document.getElementById("client-error-overlay");
        expect(overlay?.textContent).toBe("Client Error");

        harness.succeed();
        expect(dom.window.document.getElementById("client-error-overlay")).toBe(overlay);
    });

    it("reloads when the server instance changes", () => {
        const harness = createHarness();
        harness.setServerId("server-a");

        expect(harness.serverStateIsCurrent({ serverId: "server-a", clientAlive: true })).toBe(
            true
        );
        expect(harness.serverStateIsCurrent({ serverId: "server-b", clientAlive: true })).toBe(
            false
        );
        expect(dom.window.__reloadCount).toBe(1);
    });

    it("reloads when the current server no longer owns the client", () => {
        const harness = createHarness();
        harness.setServerId("server-a");

        expect(harness.serverStateIsCurrent({ serverId: "server-a", clientAlive: false })).toBe(
            false
        );
        expect(dom.window.__reloadCount).toBe(1);
    });
});
