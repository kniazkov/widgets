import { JSDOM } from "jsdom";
import { expect, it } from "vitest";
import { readScript } from "./helpers/scripts.js";

function fixture() {
    const dom = new JSDOM("<!doctype html><body></body>", { runScripts: "outside-only" });
    dom.window.eval(`
        const widgets = {};
        const sent = [];
        function listenWidget(node, type, fn) { node.addEventListener(type, fn); }
        function initPointerEvents() {}
        function initFocusEvents() {}
        function sendEventToServer(node, type, value) { sent.push(value.text); }
        ${readScript("widgets/inputs.js")}
        ${readScript("widgets/temporal.js")}
        window.testTemporal = { createTemporalField, setTemporalInput, widgets, sent };
    `);
    return { dom, ...dom.window.testTemporal };
}

it("distinguishes an incomplete native edit from clear without timezone arithmetic", () => {
    const { dom, createTemporalField, sent } = fixture();
    try {
        const date = createTemporalField("date");
        dom.window.document.body.append(date);
        date.value = "2028-02-29";
        date.dispatchEvent(new dom.window.Event("input"));
        expect(sent.at(-1)).toBe("2028-02-29");
        date.value = "";
        Object.defineProperty(date, "validity", { configurable: true, value: { badInput: true } });
        date.dispatchEvent(new dom.window.Event("input"));
        expect(sent.at(-1)).toBe("?");
        date._deferredText = "2028-02-29";
        expect(date.setText("?")).toBe(false);
        expect(date._deferredText).toBeNull();
        expect(date.validity.badInput).toBe(true);
        Object.defineProperty(date, "validity", { value: { badInput: false } });
        date.dispatchEvent(new dom.window.Event("input"));
        expect(sent.at(-1)).toBe("");
    } finally {
        dom.window.close();
    }
});

it("applies bounds and language atomically without changing focused input", () => {
    const { dom, createTemporalField, setTemporalInput, widgets } = fixture();
    try {
        const time = (widgets[1] = createTemporalField("time"));
        dom.window.document.body.append(time);
        time.value = "12:30";
        time.focus();
        expect(time.step).toBe("60");
        expect(
            setTemporalInput({ widget: 1, snapshot: { min: "09:00", max: "18:00", lang: "ru" } })
        ).toBe(true);
        expect(time.value).toBe("12:30");
        expect(time.min).toBe("09:00");
        expect(time.lang).toBe("ru");
        expect(
            setTemporalInput({ widget: 1, snapshot: { min: "bad", max: "", lang: "en-US" } })
        ).toBe(false);
        expect(time.lang).toBe("ru");
        expect(setTemporalInput({ widget: 1, snapshot: { min: "", max: "", lang: "en-US" } })).toBe(
            true
        );
        expect(time.min).toBe("");
        expect(time.value).toBe("12:30");
    } finally {
        dom.window.close();
    }
});
