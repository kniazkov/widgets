import { JSDOM } from "jsdom";
import { expect, it } from "vitest";
import { readScriptGroup } from "./helpers/scripts.js";

it("previews exactly, bounds event traffic and retains authoritative state on cancellation", () => {
    const dom = new JSDOM("<body></body>", { runScripts: "outside-only" });
    try {
        dom.window.eval(`
            const commits = [];
            function sendEventToServer(widget, type, data) { commits.push(data.index); }
            ${readScriptGroup("shared")}
            ${readScriptGroup("widgets")}
            window.sliderApi = {createWidget, widgets, setSlider, commits};
        `);
        const { createWidget, widgets, setSlider, commits } = dom.window.sliderApi;
        createWidget({ widget: "#1", type: "slider" });
        const widget = widgets["#1"];
        dom.window.document.body.append(widget);
        const input = widget.querySelector("input");
        const snapshot = {
            base: "900719925474099301",
            step: "1",
            scale: 2,
            ticks: 100,
            index: 0,
            unit: "mm"
        };
        const set = index => setSlider({ widget: "#1", snapshot: { ...snapshot, index } });
        expect(set(0)).toBe(true);
        input.setPointerCapture = () => {};
        input.hasPointerCapture = () => false;
        const pointer = type => {
            const e = new dom.window.Event(type);
            Object.assign(e, { pointerId: 7, isPrimary: true, button: 0 });
            input.dispatchEvent(e);
        };
        pointer("pointerdown");
        for (let i = 1; i <= 99; i++) {
            input.value = String(i);
            input.dispatchEvent(new dom.window.Event("input"));
            input.dispatchEvent(new dom.window.Event("change"));
        }
        expect(commits).toEqual([]);
        expect(input.getAttribute("aria-valuetext")).toBe("9007199254740994.00 mm");
        pointer("pointerup");
        input.dispatchEvent(new dom.window.Event("change"));
        expect(commits).toEqual([99]);
        pointer("pointerdown");
        set(4);
        input.value = "88";
        pointer("pointercancel");
        expect(input.value).toBe("4");
        expect(input.getAttribute("aria-valuetext")).toBe("9007199254740993.05 mm");
        expect(commits).toEqual([99]);
    } finally {
        dom.window.close();
    }
});
