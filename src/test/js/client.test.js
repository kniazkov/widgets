import { readScriptGroup } from "./helpers/scripts.js";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const source = readScriptGroup("client");

const handlerNames = [
    "log",
    "setCountdownDeadline",
    "sampleCountdownClock",
    "disposeCountdowns",
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
    "disposeWidget",
    "disposeAllWidgets",
    "trimDetachedWidgets",
    "setValidFlag",
    "setDisabledFlag",
    "setHiddenFlag",
    "setText",
    "setPlaceholder",
    "setPlaceholderColor",
    "setInputMode",
    "setSuggestions",
    "setSuggestionSeparator",
    "setOptions",
    "setOption",
    "setCarouselSources",
    "setCarouselSource",
    "setSelectedIndex",
    "setChildOrder",
    "setMaxScale",
    "setFitContent",
    "setFitToFirstChild",
    "setAnimationDuration",
    "resetZoom",
    "setHref",
    "setColor",
    "setBgColor",
    "setBackdropColor",
    "setCloseOnOutsideClick",
    "setOpacity",
    "setWidth",
    "setMaxWidth",
    "setMaxHeight",
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
    "setIntrinsicSize",
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
        window.setTimeout = function (callback, delay) {
            const task = function () {
                if (!task.active) return;
                task.active = false;
                callback();
            };
            task.delay = delay;
            task.active = true;
            window.__scheduledTasks.push(task);
            return task;
        };
        window.clearTimeout = function (task) {
            if (task) task.active = false;
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
            showClientError,
            setClientId: value => { clientId = value; },
            startClient,
            requests: window.__requests,
            scheduledTasks: window.__scheduledTasks,
            setServerId: function (value) { serverId = value; },
            serverStateIsCurrent: serverStateIsCurrent,
            reconcileTextInputs,
            openPageInNewTab,
            sendEventToServer,
            createEvent,
            sendSynchronizeRequest,
            mainCycle,
            disposeClient,
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

    it("reports a browser failure once for an initialized client", () => {
        const harness = createHarness();
        harness.setClientId("#123");
        const error = new Error("broken widget");
        error.name = "SecurityError";
        error.stack = "replaceState@[native code]\nsaveScroll@navigation.js:28";
        harness.showClientError(error);
        harness.showClientError(new Error("second error"));
        const reports = harness.requests.filter(entry => entry.request.action === "report error");
        expect(reports).toHaveLength(1);
        expect(reports[0].request.client).toBe("#123");
        expect(reports[0].request.error).toContain("broken widget");
        expect(reports[0].request.error).toContain("SecurityError: broken widget");
        expect(reports[0].request.error).toContain("replaceState@[native code]");
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

describe("event delivery acknowledgements", () => {
    function deliveryHarness() {
        const harness = createHarness();
        harness.setClientId("#123");
        harness.setServerId("server");
        harness.enqueue = () => harness.createEvent({ _id: "#7" }, "click");
        harness.sync = harness.sendSynchronizeRequest;
        harness.respond = (lastEvent, extra = {}) => {
            harness.requests.at(-1).callback(
                JSON.stringify({
                    result: true,
                    clientAlive: true,
                    serverId: "server",
                    lastEvent,
                    ...extra
                })
            );
        };
        return harness;
    }

    it("consumes rejected/obsolete events and leaves subsequent in-flight input queued", () => {
        const harness = deliveryHarness();
        harness.enqueue();
        harness.sync();
        harness.enqueue();
        expect(harness.requests[0].request.events).toHaveLength(1);
        harness.respond("#1", { eventResults: [{ id: "#1", status: "obsolete" }] });
        expect(harness.events.map(event => event.id)).toEqual(["#2"]);
        expect(harness.requests).toHaveLength(2);
        harness.respond("#2", { eventResults: [{ id: "#2", status: "rejected" }] });
        expect(harness.events).toHaveLength(0);
        expect(harness.requests).toHaveLength(2);
        expect(harness.scheduledTasks).toHaveLength(0);
    });

    it("bounds unchanged ACKs and gates polling, new input and recursive callbacks", () => {
        const harness = deliveryHarness();
        harness.enqueue();
        const results = [];
        const again = accepted => {
            results.push(accepted);
            harness.sync(again);
        };
        harness.sync(again);
        harness.respond("#?");
        expect(results).toEqual([false]);
        expect(harness.requests).toHaveLength(1);
        expect(harness.scheduledTasks[0].delay).toBe(250);
        harness.enqueue();
        harness.mainCycle();
        harness.sync();
        expect(harness.requests).toHaveLength(1);
        harness.scheduledTasks[0]();
        harness.respond("#?");
        expect(harness.scheduledTasks[1].delay).toBe(500);
        harness.scheduledTasks[1]();
        harness.respond("#?");
        expect(harness.requests).toHaveLength(3);
        expect(dom.window.__reloadCount).toBe(1);
        harness.sync();
        harness.mainCycle();
        for (const task of harness.scheduledTasks) task();
        expect(harness.requests).toHaveLength(3);
        expect(results.every(value => !value)).toBe(true);
    });

    it("resets the stall budget after partial cumulative progress", () => {
        const harness = deliveryHarness();
        harness.enqueue();
        harness.enqueue();
        harness.sync();
        harness.respond("#?");
        harness.scheduledTasks[0]();
        harness.respond("#1");
        expect(harness.events.map(event => event.id)).toEqual(["#2"]);
        harness.respond("#1");
        expect(harness.scheduledTasks[1].delay).toBe(250);
        harness.scheduledTasks[1]();
        harness.respond("#2");
        expect(harness.events).toHaveLength(0);
        expect(dom.window.__reloadCount).toBe(0);
    });

    it("retries a lost reply with the same IDs after the polling delay", () => {
        const harness = deliveryHarness();
        harness.enqueue();
        harness.sync();
        harness.requests[0].callback(null);
        harness.enqueue();
        harness.sync();
        expect(harness.requests).toHaveLength(1);
        expect(harness.scheduledTasks[0].delay).toBe(2500);
        harness.scheduledTasks[0]();
        expect(harness.requests[1].request.events.map(event => event.id)).toEqual(["#1", "#2"]);
        harness.respond("#2");
        expect(harness.events).toHaveLength(0);
        expect(dom.window.__reloadCount).toBe(0);
    });

    it.each([undefined, "broken", "#", "#01", "#1.0", "#2", "#9007199254740992"])(
        "resynchronizes an invalid or unsent ACK %s without dropping pending input",
        ack => {
            const harness = deliveryHarness();
            harness.enqueue();
            harness.sync();
            harness.enqueue();
            harness.respond(ack);
            expect(dom.window.__reloadCount).toBe(1);
            expect(harness.events).toHaveLength(2);
            expect(harness.requests).toHaveLength(1);
        }
    );

    it("resynchronizes a regressing ACK even on an empty poll", () => {
        const harness = deliveryHarness();
        harness.enqueue();
        harness.sync();
        harness.respond("#1");
        harness.sync();
        harness.respond("#?");
        expect(dom.window.__reloadCount).toBe(1);
        expect(harness.requests).toHaveLength(2);
    });

    it("honors server resync and closes the gate before invoking callbacks", () => {
        const harness = deliveryHarness();
        harness.enqueue();
        harness.sync(() => harness.sync());
        harness.respond("#?", { eventResyncRequired: true });
        expect(dom.window.__reloadCount).toBe(1);
        expect(harness.requests).toHaveLength(1);
    });

    it("cancels retry timers and ignores late replies after disposal", () => {
        const harness = deliveryHarness();
        harness.enqueue();
        harness.sync();
        const reply = harness.requests[0].callback;
        harness.respond("#?");
        harness.disposeClient();
        harness.scheduledTasks[0]();
        reply(
            JSON.stringify({ result: true, serverId: "server", clientAlive: true, lastEvent: "#1" })
        );
        expect(harness.requests.map(entry => entry.request.action)).toEqual([
            "synchronize",
            "kill"
        ]);
        expect(dom.window.__reloadCount).toBe(0);
        expect(harness.events).toHaveLength(1);
    });
});
