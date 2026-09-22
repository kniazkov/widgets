import fs from "node:fs";
import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

let dom;
afterEach(() => dom?.window.close());
function harness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    const read = file =>
        fs.readFileSync(new URL(`../../main/html/scripts/${file}`, import.meta.url), "utf8");
    dom.window.eval(`${read("options.js")}\n${read("lib.js")}\n
        window.events = [];
        function sendEventToServer(widget, type, data) { window.events.push({ type, data }); }
        ${read("widgets.js")}
        window.h = { createWidget, appendChildWidget, setChildWidget, removeChildWidget, setChildOrder, setMaxScale, setAnimationDuration, resetZoom, widgets };
    `);
    const h = dom.window.h;
    h.make = (type, id) => {
        h.createWidget({ type, widget: id });
        return h.widgets[id];
    };
    h.events = dom.window.events;
    return h;
}
function pointer(target, type, x, y = 20, id = 1, pointerType = "touch") {
    const e = new dom.window.Event(type, { bubbles: true, cancelable: true });
    Object.assign(e, {
        clientX: x,
        clientY: y,
        pointerId: id,
        pointerType,
        button: 0,
        isPrimary: id === 1
    });
    target.dispatchEvent(e);
    return e;
}
function sortable() {
    const h = harness();
    const box = h.make("sortable section", "sort");
    h.setAnimationDuration({ widget: "sort", "animation duration": 250 });
    dom.window.document.body.append(box);
    const items = ["a", "b", "c"].map(id => {
        const child = h.make("text", id);
        h.appendChildWidget({ widget: id, container: "sort" });
        child.getBoundingClientRect = () => {
            const index = Array.from(box.children).indexOf(child);
            const translate = child.style.transform.match(/translate\(([-\d.]+)px, ([-\d.]+)px\)/);
            const dx = translate ? Number(translate[1]) : 0;
            const dy = translate ? Number(translate[2]) : 0;
            return {
                left: index * 100 + dx,
                right: (index + 1) * 100 + dx,
                top: dy,
                bottom: 60 + dy,
                width: 100,
                height: 60
            };
        };
        return child;
    });
    h.setChildOrder({ widget: "sort", children: ["a", "b", "c"], revision: 3 });
    return { h, box, items, order: () => Array.from(box.children, child => child._id) };
}
function zoom() {
    const h = harness();
    const box = h.make("zoom decorator", "zoom");
    h.setMaxScale({ widget: "zoom", "max scale": 8 });
    const child = h.make("text", "content");
    dom.window.document.body.append(box);
    h.setChildWidget({ widget: "content", container: "zoom" });
    Object.defineProperties(box, { clientWidth: { value: 300 }, clientHeight: { value: 200 } });
    Object.defineProperties(box._stage, {
        offsetWidth: { value: 300 },
        offsetHeight: { value: 200 }
    });
    box.getBoundingClientRect = () => ({ left: 0, top: 0 });
    const wheel = (delta, mode = 0) => {
        const event = new dom.window.WheelEvent("wheel", {
            deltaY: delta,
            deltaMode: mode,
            clientX: 150,
            clientY: 100,
            cancelable: true
        });
        box.dispatchEvent(event);
        return event;
    };
    return { h, box, child, wheel, transform: () => box._stage.style.transform };
}

describe("sortable inline widgets", () => {
    it.each(["mouse", "touch"])(
        "moves with %s and emits one versioned request on release",
        type => {
            const { h, box, items, order } = sortable();
            pointer(items[0], "pointerdown", 20, 20, 1, type);
            pointer(box, "pointermove", 280, 20, 1, type);
            expect(order()).toEqual(["a", "b", "c"]);
            expect(items[0].getBoundingClientRect().left).toBe(260);
            expect(items[0].style.zIndex).toBe("1000");
            expect(items[1].getBoundingClientRect().left).toBe(100);
            expect(h.events).toHaveLength(0);
            pointer(box, "pointerup", 280, 20, 1, type);
            expect(h.events).toEqual([
                { type: "reorder", data: { child: "a", before: "", revision: 3 } }
            ]);
            expect(box.lastElementChild).toBe(items[0]);
            expect(items[0].style.transform).toBe("");
        }
    );
    it.each(["pointercancel", "lostpointercapture"])("rolls back on %s", type => {
        const { h, box, items, order } = sortable();
        pointer(items[0], "pointerdown", 20);
        pointer(box, "pointermove", 280);
        pointer(box, type, 280);
        expect(order()).toEqual(["a", "b", "c"]);
        expect(items[0].style.opacity).toBe("");
        expect(h.events).toHaveLength(0);
    });
    it("does not reorder taps or start drags from nested controls", () => {
        const { h, box, items, order } = sortable();
        pointer(items[0], "pointerdown", 20);
        pointer(box, "pointermove", 22);
        pointer(box, "pointerup", 22);
        const button = dom.window.document.createElement("button");
        items[0].append(button);
        pointer(button, "pointerdown", 20);
        pointer(box, "pointermove", 280);
        pointer(box, "pointerup", 280);
        expect(order()).toEqual(["a", "b", "c"]);
        expect(h.events).toHaveLength(0);
    });
    it("suppresses clicks after drag and allows the next ordinary click", () => {
        const { box, items } = sortable();
        let clicks = 0;
        items[0].addEventListener("click", () => clicks++);
        pointer(items[0], "pointerdown", 20);
        pointer(box, "pointermove", 280);
        pointer(box, "pointerup", 280);
        items[0].click();
        expect(clicks).toBe(0);
        pointer(items[0], "pointerdown", 20);
        pointer(box, "pointerup", 20);
        items[0].click();
        expect(clicks).toBe(1);
    });
    it("accepts authoritative order during a drag and rejects invalid permutations", () => {
        const { h, box, items, order } = sortable();
        pointer(items[0], "pointerdown", 20);
        pointer(box, "pointermove", 280);
        expect(h.setChildOrder({ widget: "sort", children: ["c", "b", "a"], revision: 4 })).toBe(
            true
        );
        pointer(box, "pointerup", 280);
        expect(order()).toEqual(["c", "b", "a"]);
        expect(h.events).toHaveLength(0);
        expect(h.setChildOrder({ widget: "sort", children: ["a", "a", "c"], revision: 5 })).toBe(
            false
        );
        expect(
            h.setChildOrder({ widget: "sort", children: ["a", "missing", "c"], revision: 5 })
        ).toBe(false);
        expect(order()).toEqual(["c", "b", "a"]);
    });
    it("animates the drop and siblings with the configured duration without restarting on ack", () => {
        const { h, box, items } = sortable();
        const animations = [];
        for (const child of items)
            child.animate = (frames, options) => {
                const animation = {
                    child,
                    frames,
                    options,
                    cancelled: false,
                    cancel() {
                        this.cancelled = true;
                    }
                };
                animations.push(animation);
                return animation;
            };
        expect(h.setAnimationDuration({ widget: "sort", "animation duration": 600 })).toBe(true);
        pointer(items[0], "pointerdown", 20);
        pointer(box, "pointermove", 280);
        expect(animations).toHaveLength(0);
        pointer(box, "pointerup", 280);
        expect(animations).toHaveLength(3);
        expect(animations.every(animation => animation.options.duration === 600)).toBe(true);
        expect(
            animations.find(animation => animation.child === items[1]).frames[0].transform
        ).toContain("translate(100px, 0px)");
        expect(items[0].style.zIndex).toBe("1000");
        h.setChildOrder({ widget: "sort", children: ["b", "c", "a"], revision: 4 });
        expect(animations).toHaveLength(3);
        expect(animations.every(animation => !animation.cancelled)).toBe(true);
        for (const animation of animations) animation.onfinish();
        expect(items[0].style.zIndex).toBe("");
        expect(items[0].style.transform).toBe("");
    });
    it.each([0, 250])("honors zero duration and reduced motion (%i)", duration => {
        const { h, box, items, order } = sortable();
        h.setAnimationDuration({ widget: "sort", "animation duration": duration });
        dom.window.matchMedia = () => ({ matches: duration !== 0 });
        for (const child of items)
            child.animate = () => {
                throw new Error("Must not animate");
            };
        pointer(items[0], "pointerdown", 20);
        pointer(box, "pointermove", 280);
        expect(items[0].getBoundingClientRect().left).toBe(260);
        pointer(box, "pointerup", 280);
        expect(order()).toEqual(["b", "c", "a"]);
        expect(items[0].style.transform).toBe("");
    });
    it("validates duration and clears running animations when disabled or detached", () => {
        const { h, box, items } = sortable();
        expect(box._animationDuration).toBe(250);
        for (const invalid of [-1, 0.5, NaN, Infinity, "200"]) {
            expect(h.setAnimationDuration({ widget: "sort", "animation duration": invalid })).toBe(
                false
            );
        }
        let cancelled = 0;
        for (const child of items)
            child.animate = () => ({
                cancel() {
                    cancelled++;
                }
            });
        pointer(items[0], "pointerdown", 20);
        pointer(box, "pointermove", 280);
        pointer(box, "pointerup", 280);
        h.setAnimationDuration({ widget: "sort", "animation duration": 0 });
        expect(cancelled).toBe(3);
        expect(items[0].style.zIndex).toBe("");
        box._onDetached();
        expect(cancelled).toBe(3);
    });
    it("restores custom child transforms, transitions and stacking after cancellation", () => {
        const { box, items, order } = sortable();
        items[0].style.transform = "scale(1.2)";
        items[0].style.transition = "transform 1s";
        items[0].style.zIndex = "4";
        pointer(items[0], "pointerdown", 20);
        pointer(box, "pointermove", 280);
        expect(items[0].style.transform).toContain("scale(1.2)");
        pointer(box, "pointercancel", 280);
        expect(order()).toEqual(["a", "b", "c"]);
        expect(items[0].style.transform).toBe("scale(1.2)");
        expect(items[0].style.transition).toBe("transform 1s");
        expect(items[0].style.zIndex).toBe("4");
    });
    it("supports keyboard reordering", () => {
        const { h, items, order } = sortable();
        items[0].dispatchEvent(
            new dom.window.KeyboardEvent("keydown", {
                key: "ArrowRight",
                altKey: true,
                bubbles: true
            })
        );
        expect(order()).toEqual(["b", "a", "c"]);
        expect(h.events[0].data.before).toBe("c");
    });
    it("waits for acknowledgement and keeps keyboard focus after the server update", () => {
        const { h, box, items, order } = sortable();
        items[0].focus();
        items[0].dispatchEvent(
            new dom.window.KeyboardEvent("keydown", {
                key: "ArrowRight",
                altKey: true,
                bubbles: true
            })
        );
        pointer(items[1], "pointerdown", 20);
        pointer(box, "pointermove", 280);
        pointer(box, "pointerup", 280);
        expect(h.events).toHaveLength(1);
        expect(order()).toEqual(["b", "a", "c"]);
        h.setChildOrder({ widget: "sort", children: ["b", "a", "c"], revision: 4 });
        expect(dom.window.document.activeElement).toBe(items[0]);
        items[0].dispatchEvent(
            new dom.window.KeyboardEvent("keydown", {
                key: "ArrowRight",
                altKey: true,
                bubbles: true
            })
        );
        expect(h.events).toHaveLength(2);
        expect(h.events[1].data.revision).toBe(4);
    });

    it("supports wrapped rows", () => {
        const { box, items, order } = sortable();
        items[2].getBoundingClientRect = () => ({
            left: 0,
            right: 100,
            top: 70,
            bottom: 130,
            width: 100
        });
        pointer(items[0], "pointerdown", 20);
        pointer(box, "pointermove", 80, 100);
        pointer(box, "pointerup", 80, 100);
        expect(order()).toEqual(["b", "c", "a"]);
    });
});

describe("generic zoom decorator", () => {
    it("zooms around the cursor, clamps scale and resets through the protocol", () => {
        const { h, box, wheel, transform } = zoom();
        expect(wheel(-Math.log(2) / 0.002).defaultPrevented).toBe(true);
        expect(transform()).toBe("translate(-150px, -100px) scale(2)");
        wheel(-10000);
        wheel(-10000);
        expect(transform()).toContain("scale(8)");
        expect(h.setMaxScale({ widget: "zoom", "max scale": 3 })).toBe(true);
        expect(transform()).toBe("translate(0px, 0px) scale(1)");
        wheel(-10000);
        expect(transform()).toContain("scale(3)");
        expect(h.setMaxScale({ widget: "zoom", "max scale": Infinity })).toBe(false);
        expect(box._maxScale).toBe(3);
        expect(h.events).toHaveLength(0);
    });
    it("resets the viewport without overwriting the scale property", () => {
        const { h, box, wheel, transform } = zoom();
        h.setMaxScale({ widget: "zoom", "max scale": 3 });
        wheel(-10000);
        expect(transform()).toContain("scale(3)");
        expect(h.resetZoom({ widget: "zoom" })).toBe(true);
        expect(box._maxScale).toBe(3);
        expect(transform()).toContain("scale(1)");
    });
    it("normalizes line and page wheel units and never shrinks below one", () => {
        const { wheel, transform } = zoom();
        wheel(-1, 1);
        expect(transform()).not.toContain("scale(1)");
        wheel(10, 2);
        expect(transform()).toContain("scale(1)");
    });
    it("pans within bounds and suppresses the resulting child click", () => {
        const { box, child, wheel, transform } = zoom();
        wheel(-Math.log(2) / 0.002);
        let clicks = 0;
        child.addEventListener("click", () => clicks++);
        pointer(child, "pointerdown", 150, 100);
        pointer(box, "pointermove", -1000, -1000);
        expect(transform()).toBe("translate(-300px, -200px) scale(2)");
        pointer(box, "pointerup", -1000, -1000);
        child.click();
        expect(clicks).toBe(0);
        pointer(child, "pointerdown", 50, 50);
        pointer(box, "pointerup", 50, 50);
        child.click();
        expect(clicks).toBe(1);
    });
    it("pinches, continues with one finger, and clears cancelled pointers", () => {
        const { box, transform } = zoom();
        pointer(box, "pointerdown", 100, 100, 1);
        pointer(box, "pointerdown", 200, 100, 2);
        pointer(box, "pointermove", 300, 100, 2);
        expect(transform()).toContain("scale(2)");
        pointer(box, "pointerup", 300, 100, 2);
        pointer(box, "pointermove", 80, 100, 1);
        expect(transform()).toBe("translate(-120px, -100px) scale(2)");
        pointer(box, "pointercancel", 80, 100, 1);
        pointer(box, "pointermove", 10, 100, 1);
        expect(transform()).toBe("translate(-120px, -100px) scale(2)");
    });
    it("routes child removal through the private content host", () => {
        const { h, box } = zoom();
        expect(h.removeChildWidget({ widget: "content", container: "zoom" })).toBe(true);
        expect(box.firstElementChild).toBe(box._stage);
        expect(box._stage.children).toHaveLength(0);
    });

    it("replaces arbitrary content without losing its transform wrapper", () => {
        const { h, box, wheel, transform } = zoom();
        wheel(-200);
        const next = h.make("inline block", "next");
        h.setChildWidget({ widget: "next", container: "zoom" });
        expect(box.firstElementChild).toBe(box._stage);
        expect(box._stage.firstElementChild).toBe(next);
        expect(transform()).toBe("translate(0px, 0px) scale(1)");
    });
});
