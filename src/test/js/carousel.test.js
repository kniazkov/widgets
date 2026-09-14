import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const optionsSource = fs.readFileSync(
    new URL("../../main/html/scripts/options.js", import.meta.url),
    "utf8"
);
const librarySource = fs.readFileSync(
    new URL("../../main/html/scripts/lib.js", import.meta.url),
    "utf8"
);
const widgetsSource = fs.readFileSync(
    new URL("../../main/html/scripts/widgets.js", import.meta.url),
    "utf8"
);

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
        window.__carouselEvents = [];
        let clientId = "#1";
        function createEvent(widget, type, data) {
            window.__carouselEvents.push({ widget, type, data });
        }
        function sendSynchronizeRequest() {}
        function sendEventToServer(widget, type, data) {
            if (widget._events[type] || ["select"].includes(type)) {
                createEvent(widget, type, data);
            }
        }
        ${widgetsSource}
        window.__carouselHarness = {
            createWidget,
            subscribeToEvent,
            setCarouselSources,
            setCarouselSource,
            setSelectedIndex,
            widgets
        };
    `);
    return {
        ...dom.window.__carouselHarness,
        events: dom.window.__carouselEvents
    };
}

function pointer(widget, type, x, y = 20) {
    const event = new dom.window.Event(type, { bubbles: true, cancelable: true });
    Object.defineProperties(event, {
        pointerId: { value: 1 },
        isPrimary: { value: true },
        pointerType: { value: "touch" },
        button: { value: 0 },
        buttons: { value: type == "pointerup" ? 0 : 1 },
        clientX: { value: x },
        clientY: { value: y },
        pageX: { value: x },
        pageY: { value: y },
        screenX: { value: x },
        screenY: { value: y },
        pressure: { value: type == "pointerup" ? 0 : 0.5 }
    });
    widget.dispatchEvent(event);
}

describe("carousel", () => {
    it("renders sources and follows server-side selection", () => {
        const harness = createHarness();
        const id = "#40";
        expect(harness.createWidget({ type: "carousel", widget: id })).toBe(true);
        expect(
            harness.setCarouselSources({ widget: id, sources: ["first.png", "second.png"] })
        ).toBe(true);
        const widget = harness.widgets[id];
        expect(widget._image.getAttribute("src")).toBe("first.png");

        expect(harness.setSelectedIndex({ widget: id, "selected index": 1 })).toBe(true);
        expect(widget._image.getAttribute("src")).toBe("second.png");
        expect(harness.setCarouselSource({ widget: id, index: 1, source: "changed.png" })).toBe(
            true
        );
        expect(widget._image.getAttribute("src")).toBe("changed.png");
        expect(harness.setSelectedIndex({ widget: id, "selected index": -1 })).toBe(false);
        expect(harness.setCarouselSources({ widget: id, sources: [] })).toBe(false);
    });

    it("selects adjacent images with horizontal pointer swipes", () => {
        const harness = createHarness();
        const id = "#41";
        harness.createWidget({ type: "carousel", widget: id });
        harness.setCarouselSources({ widget: id, sources: ["first.png", "second.png"] });
        const widget = harness.widgets[id];
        widget.getBoundingClientRect = () => ({ left: 0, top: 0, width: 300, height: 200 });

        pointer(widget, "pointerdown", 250);
        pointer(widget, "pointermove", 100);
        pointer(widget, "pointerup", 100);

        expect(widget._selectedIndex).toBe(1);
        expect(widget._image.getAttribute("src")).toBe("second.png");
        expect(harness.events).toEqual([{ widget, type: "select", data: { index: 1 } }]);
    });

    it("resists end swipes by at most one third and suppresses their synthetic click", () => {
        const harness = createHarness();
        const id = "#42";
        harness.createWidget({ type: "carousel", widget: id });
        harness.setCarouselSources({ widget: id, sources: ["first.png", "second.png"] });
        harness.subscribeToEvent({ widget: id, event: "click" });
        const widget = harness.widgets[id];
        widget.getBoundingClientRect = () => ({ left: 0, top: 0, width: 300, height: 200 });

        pointer(widget, "pointerdown", 0);
        pointer(widget, "pointermove", 900);
        expect(widget._image.style.transform).toBe("translateX(100px)");
        pointer(widget, "pointerup", 900);
        expect(widget._image.style.transform).toBe("translateX(0px)");
        widget.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true }));

        expect(widget._selectedIndex).toBe(0);
        expect(harness.events).toEqual([]);
    });

    it("reports an ordinary click when no swipe occurred", () => {
        const harness = createHarness();
        const id = "#43";
        harness.createWidget({ type: "carousel", widget: id });
        harness.setCarouselSources({ widget: id, sources: ["only.png"] });
        harness.subscribeToEvent({ widget: id, event: "click" });
        const widget = harness.widgets[id];

        widget.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true }));

        expect(harness.events).toHaveLength(1);
        expect(harness.events[0].type).toBe("click");
    });
});
