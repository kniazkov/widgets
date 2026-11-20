import { JSDOM } from "jsdom";
import { expect, it } from "vitest";
import { readScript } from "./helpers/scripts.js";

it("releases visual viewport listeners on detach/dispose and reinstalls only one backdrop", () => {
    const dom = new JSDOM("<!doctype html><body></body>", { runScripts: "outside-only" });
    try {
        const viewport = new dom.window.EventTarget();
        Object.assign(viewport, { width: 390, height: 300, offsetLeft: 0, offsetTop: 100 });
        Object.defineProperty(dom.window, "visualViewport", { value: viewport });
        dom.window.eval(`
            function listenWidget(widget, event, fn, capture, target = widget) {
                target.addEventListener(event, fn, capture);
            }
            ${readScript("widgets/popup.js")}
            window.createTestDialog = createDialog;
        `);
        const widget = dom.window.createTestDialog();
        dom.window.document.body.append(widget);
        widget._onAttached();
        expect(widget.style.top).toBe("112px");
        expect(widget.style.getPropertyValue("--dialog-height")).toBe("300px");
        viewport.height = 250;
        viewport.dispatchEvent(new dom.window.Event("resize"));
        expect(widget.style.getPropertyValue("--dialog-height")).toBe("250px");
        widget._onDetached();
        viewport.height = 200;
        viewport.dispatchEvent(new dom.window.Event("resize"));
        expect(widget.style.getPropertyValue("--dialog-height")).toBe("250px");
        widget._onAttached();
        widget._onAttached();
        expect(dom.window.document.querySelectorAll(".popup-backdrop")).toHaveLength(1);
        expect(widget.style.getPropertyValue("--dialog-height")).toBe("200px");
        widget._onDisposed();
        viewport.height = 150;
        viewport.dispatchEvent(new dom.window.Event("scroll"));
        dom.window.dispatchEvent(new dom.window.Event("resize"));
        expect(widget.style.getPropertyValue("--dialog-height")).toBe("200px");
    } finally {
        dom.window.close();
    }
});
