import { readScript, readScriptGroup } from "./helpers/scripts.js";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const optionsSource = readScript("options.js");
const librarySource = readScriptGroup("shared");
const widgetsSource = readScriptGroup("widgets");

let dom;

afterEach(() => {
    dom?.window.close();
});

function createHarness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`${optionsSource}\n${librarySource}\n
        configureUploadProtocol(4 * 1024, 128 * 1024 * 1024);
        window.__focusEvents = [];
        let clientId = "#1";
        function createEvent(widget, type, data) {
            window.__focusEvents.push({ widget, type, data });
        }
        function sendSynchronizeRequest() {}
        function sendEventToServer(widget, type, data) {
            if (widget._events[type]) {
                createEvent(widget, type, data);
                sendSynchronizeRequest();
            }
        }
        ${widgetsSource}
        window.__focusHarness = {
            createWidget,
            getWidgetProperty,
            subscribeToEvent,
            setHref,
            widgets
        };
    `);
    return {
        ...dom.window.__focusHarness,
        events: dom.window.__focusEvents
    };
}

describe("focused widget state", () => {
    it.each([
        "input field",
        "password input",
        "text area",
        "drop down list",
        "button",
        "file loader",
        "link"
    ])("tracks focus and blur for %s", type => {
        const harness = createHarness();
        const id = "#7";
        harness.createWidget({ type, widget: id });
        harness.subscribeToEvent({ widget: id, event: "focus" });
        harness.subscribeToEvent({ widget: id, event: "blur" });
        const widget = harness.widgets[id];
        widget._properties.normal.marker = "normal";
        widget._properties.focused.marker = "focused";
        dom.window.document.body.appendChild(widget);

        widget.focus();

        expect(widget._states.focused).toBe(true);
        expect(harness.getWidgetProperty(widget, "marker")).toBe("focused");
        expect(harness.events.map(event => event.type)).toEqual(["focus"]);

        widget.blur();

        expect(widget._states.focused).toBe(false);
        expect(harness.getWidgetProperty(widget, "marker")).toBe("normal");
        expect(harness.events.map(event => event.type)).toEqual(["focus", "blur"]);
    });

    it("creates links as anchors and updates their destinations", () => {
        const harness = createHarness();
        const id = "#9";

        expect(harness.createWidget({ type: "link", widget: id })).toBe(true);
        const widget = harness.widgets[id];
        expect(widget.tagName).toBe("A");
        expect(widget.getAttribute("href")).toBe("#");

        expect(harness.setHref({ widget: id, href: "/documentation" })).toBe(true);
        expect(widget.getAttribute("href")).toBe("/documentation");
        expect(harness.setHref({ widget: id, href: null })).toBe(false);
    });

    it.each([
        "section",
        "panel",
        "text",
        "active text",
        "image",
        "active image",
        "checkbox",
        "radio button"
    ])("does not add focus behavior to %s", type => {
        const harness = createHarness();
        const id = "#8";
        harness.createWidget({ type, widget: id });
        harness.subscribeToEvent({ widget: id, event: "focus" });
        const widget = harness.widgets[id];

        widget.dispatchEvent(new dom.window.FocusEvent("focus"));

        expect(widget._states.focused).toBe(false);
        expect(harness.events).toEqual([]);
    });
});
