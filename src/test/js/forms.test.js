import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";
import { readScriptGroup } from "./helpers/scripts.js";

let dom;

afterEach(() => dom?.window.close());

function harness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`
        function sendEventToServer() {}
        ${readScriptGroup("shared")}
        ${readScriptGroup("widgets")}
        window.testAPI = { createWidget, widgets, setAccessibleReference, refreshAccessibleRelations,
            setFormEditorError, setValidFlag, setFormEditorAttribute, focusFormEditor };
    `);
    const api = dom.window.testAPI;
    for (const [widget, type] of [
        ["#1", "form"],
        ["#2", "input field"],
        ["#3", "label"],
        ["#4", "text"],
        ["#5", "text"]
    ]) {
        expect(api.createWidget({ widget, type })).toBe(true);
    }
    const [form, input, label, help, error] = Object.values(api.widgets);
    dom.window.document.body.append(form);
    form.append(label, input, help, error);
    return { ...api, form, input, label, help, error };
}

describe("form editor semantics", () => {
    it("uses stable native label and visible help/error relations", () => {
        const h = harness();
        expect(h.form.getAttribute("role")).toBe("form");
        expect(
            h.setAccessibleReference({ widget: "#3", "label target": "#2" }, "label target")
        ).toBe(true);
        expect(h.label.control).toBe(h.input);
        h.setAccessibleReference(
            { widget: "#2", "accessible description widget": "#4" },
            "accessible description widget"
        );
        h.setAccessibleReference({ widget: "#2", "accessible error": "#5" }, "accessible error");
        expect(h.input.getAttribute("aria-describedby")).toBe(h.help.id);
        h.setFormEditorError({ widget: "#2", "form error": true });
        expect(h.input.getAttribute("aria-errormessage")).toBe(h.error.id);
        expect(h.input.getAttribute("aria-describedby")).toBe(`${h.help.id} ${h.error.id}`);
        h.setValidFlag({ widget: "#2", valid: false });
        h.setFormEditorError({ widget: "#2", "form error": false });
        expect(h.input.getAttribute("aria-invalid")).toBe("true");
        h.setValidFlag({ widget: "#2", valid: true });
        expect(h.input.hasAttribute("aria-invalid")).toBe(false);
        h.input.remove();
        h.refreshAccessibleRelations();
        expect(h.label.control).toBeNull();
    });

    it("applies native constraints and focuses only a live enabled editor", () => {
        const h = harness();
        expect(h.setFormEditorAttribute({ widget: "#2", required: true }, "required")).toBe(true);
        expect(h.setFormEditorAttribute({ widget: "#2", "max length": 40 }, "max length")).toBe(
            true
        );
        expect(h.input.maxLength).toBe(40);
        expect(h.input.required).toBe(true);
        h.input.disabled = true;
        h.focusFormEditor({ widget: "#2" });
        expect(dom.window.document.activeElement).not.toBe(h.input);
        h.input.disabled = false;
        h.focusFormEditor({ widget: "#2" });
        expect(dom.window.document.activeElement).toBe(h.input);
    });
});
