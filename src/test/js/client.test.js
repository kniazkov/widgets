import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const source = fs.readFileSync(
    new URL("../../main/html/scripts/client.js", import.meta.url),
    "utf8"
);

const handlerNames = [
    "log",
    "createWidget",
    "reset",
    "goToPage",
    "openPageInNewTab",
    "subscribeToEvent",
    "setClientEventAction",
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
    "setCarouselSources",
    "setCarouselSource",
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
    dom.window.eval(`const widgets = {};\n${handlers}\n
        window.__requests = [];
        window.__scheduledTasks = [];
        function sendRequest(request, callback) {
            window.__requests.push({ request, callback });
        }
        function isMobileDevice() {
            return false;
        }
        window.setTimeout = function (callback) {
            window.__scheduledTasks.push(callback);
            return window.__scheduledTasks.length;
        };
        ${source}\n
        window.__reloadCount = 0;
        window.__openedTabs = [];
        window.open = function (...args) {
            window.__openedTabs.push(args);
        };
        reloadCurrentPage = function () {
            window.__reloadCount++;
        };
        window.__clientHarness = {
            fail: recordRequestFailure,
            succeed: recordRequestSuccess,
            reportClientError: responseHasClientError,
            startClient,
            requests: window.__requests,
            scheduledTasks: window.__scheduledTasks,
            setServerId: function (value) { serverId = value; },
            serverStateIsCurrent: serverStateIsCurrent,
            reconcileTextInputs,
            openPageInNewTab,
            sendEventToServer,
            openedTabs: window.__openedTabs,
            events,
            widgets
        };
    `);
    return dom.window.__clientHarness;
}

describe("client creation", () => {
    it("keeps only one creation request in flight and retries after failure", () => {
        const harness = createHarness();

        harness.startClient("/catalog", { group: "all" });
        harness.startClient("/catalog", { group: "all" });

        expect(harness.requests).toHaveLength(1);
        expect(harness.requests[0].request.action).toBe("new instance");
        expect(harness.scheduledTasks).toHaveLength(0);

        harness.requests[0].callback(null);

        expect(harness.scheduledTasks).toHaveLength(1);
        harness.scheduledTasks.shift()();
        expect(harness.requests).toHaveLength(2);
    });
});

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

describe("client event actions", () => {
    it("executes an action synchronously without subscribing to the server event", () => {
        const harness = createHarness();
        const widget = {
            _id: "#7",
            _events: {},
            _clientActions: {
                click: {
                    action: "open page in new tab",
                    href: "https://example.com/original.png"
                }
            }
        };
        harness.widgets[widget._id] = widget;

        harness.sendEventToServer(widget, "click");

        expect(harness.openedTabs).toEqual([
            ["https://example.com/original.png", "_blank", "noopener"]
        ]);
        expect(harness.events).toEqual([]);
    });
});

describe("new tab navigation", () => {
    it("opens a server-provided link without granting opener access", () => {
        const harness = createHarness();

        expect(harness.openPageInNewTab({ href: "https://example.com" })).toBe(true);
        expect(harness.openedTabs).toEqual([["https://example.com", "_blank", "noopener"]]);
    });

    it("ignores a non-string href", () => {
        const harness = createHarness();

        expect(harness.openPageInNewTab({ href: 42 })).toBe(true);
        expect(harness.openedTabs).toEqual([]);
    });
});
