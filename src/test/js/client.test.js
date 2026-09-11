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
    "insertChildWidget",
    "removeChildWidget",
    "setValidFlag",
    "setDisabledFlag",
    "setHiddenFlag",
    "setText",
    "setOptions",
    "setOption",
    "setSelectedIndex",
    "setHref",
    "setColor",
    "setBgColor",
    "setBackdropColor",
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
    "setStickySide",
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
    dom.window.eval(`const widgets = {};\n${handlers}\n${source}\n
        window.__reloadCount = 0;
        reloadCurrentPage = function () {
            window.__reloadCount++;
        };
        window.__clientHarness = {
            fail: recordRequestFailure,
            succeed: recordRequestSuccess,
            reportClientError: responseHasClientError,
            setServerId: function (value) { serverId = value; },
            serverStateIsCurrent: serverStateIsCurrent,
            reconcileTextInputs,
            events,
            widgets
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

describe("text input reconciliation", () => {
    it("keeps a field pending until its last local input event is acknowledged", () => {
        const harness = createHarness();
        let applications = 0;
        const widget = {
            _id: "#7",
            _textInputPending: true,
            _applyDeferredText() {
                applications++;
            }
        };
        harness.widgets[widget._id] = widget;
        harness.events.push({ id: "#1", widget: widget._id, type: "text input" });

        harness.reconcileTextInputs();
        expect(widget._textInputPending).toBe(true);
        expect(applications).toBe(1);

        harness.events.splice(0);
        harness.reconcileTextInputs();
        expect(widget._textInputPending).toBe(false);
        expect(applications).toBe(2);
    });
});
