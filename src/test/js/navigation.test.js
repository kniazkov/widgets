import fs from "node:fs";
import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const read = name =>
    fs.readFileSync(new URL(`../../main/html/scripts/${name}.js`, import.meta.url), "utf8");
let dom;
afterEach(() => dom?.window.close());

function harness() {
    dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "http://localhost/catalog?filter=blue"
    });
    const win = dom.window;
    const requests = [];
    const intervals = new Map();
    let timer = 0;
    win.scrollTo = (x, y) => {
        win.scrollX = x;
        win.scrollY = y;
    };
    win.requestAnimationFrame = callback => callback();
    win.setInterval = callback => {
        intervals.set(++timer, callback);
        return timer;
    };
    win.clearInterval = id => intervals.delete(id);
    win.console.log = () => {};
    win.__request = (request, callback) => requests.push({ request, callback });
    win.eval(`${read("options")}\n${read("lib")}
        sendRequest = window.__request;
        isMobileDevice = () => false;
        function createPageRuntime(page) {
            ${read("widgets")}\n${read("client")}
            return { initClient, mainCycle, disposeClient, showClientError };
        }
        ${read("navigation")}
        initNavigation("session", "/catalog", {filter: "blue"}, ["/catalog", "/product"]);
    `);
    function answer(action, body, client) {
        const item = requests.find(
            item =>
                !item.answered &&
                item.request.action === action &&
                (client === undefined || item.request.client === client)
        );
        expect(item, `pending ${action} for ${client}`).toBeTruthy();
        item.answered = true;
        item.callback(JSON.stringify(body));
        return item.request;
    }
    function updates(client, values = [], extra = {}) {
        answer(
            "synchronize",
            {
                serverId: "server",
                clientAlive: true,
                result: true,
                lastEvent: "#0",
                updates: values,
                ...extra
            },
            client
        );
    }
    function created(id, text = id) {
        answer("new instance", { id, serverId: "server" });
        updates(id, [
            { id: "#1", action: "create widget", type: "root", widget: "#1" },
            { id: "#2", action: "create widget", type: "text", widget: "#2" },
            { id: "#3", action: "set text", widget: "#2", text },
            { id: "#4", action: "append child", widget: "#2", container: "#1" }
        ]);
    }
    function navigate(href) {
        const link = win.document.createElement("a");
        link.href = href;
        win.document.body.appendChild(link);
        link.click();
        link.remove();
    }
    async function back() {
        await new Promise(resolve => {
            win.addEventListener("popstate", resolve, { once: true });
            win.history.back();
        });
    }
    return { win, requests, intervals, answer, updates, created, navigate, back };
}

describe("page history cache", () => {
    it("returns the same DOM and server instance with its scroll position", async () => {
        const h = harness();
        h.created("catalog");
        const root = h.win.document.querySelector(".widgets-page");
        root.dataset.retained = "yes";
        h.win.scrollTo(0, 850);
        h.navigate("/product?id=42");
        expect(h.requests.at(-1).request).toMatchObject({
            action: "new instance",
            address: "/product",
            id: "42"
        });
        h.created("product");
        expect(root.isConnected).toBe(false);
        await h.back();
        expect(root.isConnected).toBe(false);
        h.updates("catalog");
        expect(h.win.document.querySelector(".widgets-page")).toBe(root);
        expect(h.win.scrollY).toBe(850);
        expect(h.requests.filter(x => x.request.action === "new instance")).toHaveLength(2);
        expect(h.requests.filter(x => x.request.action === "kill")).toHaveLength(0);
    });

    it("applies delayed replies to the hidden runtime and separates repeat visits", async () => {
        const h = harness();
        h.created("first");
        const root = h.win.document.querySelector(".widgets-page");
        for (const tick of h.intervals.values()) {
            tick();
        }
        h.navigate("/catalog?filter=blue");
        h.created("second");
        h.updates("first", [{ id: "#5", action: "set text", widget: "#2", text: "late" }]);
        expect(root.textContent).toBe("late");
        expect(h.win.document.body.textContent).toBe("second");
        await h.back();
        h.updates("first");
        expect(h.win.document.body.textContent).toBe("late");
    });

    it("bounds inactive pages and recreates an evicted history entry", async () => {
        const h = harness();
        h.created("first");
        for (let i = 0; i < 4; i++) {
            h.navigate(`/product?id=${i}`);
            h.created(`product${i}`);
        }
        expect(
            h.requests.filter(x => x.request.action === "kill").map(x => x.request.client)
        ).toEqual(["first"]);
        for (let i = 2; i >= 0; i--) {
            await h.back();
            h.updates(`product${i}`);
        }
        await h.back();
        expect(
            h.requests.filter(item => item.request.action === "new instance").at(-1).request
        ).toMatchObject({
            action: "new instance",
            address: "/catalog",
            filter: "blue"
        });
    });

    it("expires hidden pages by age even when no navigation happens", () => {
        const h = harness();
        h.created("first");
        h.navigate("/product");
        h.created("second");
        const now = h.win.Date.now();
        h.win.Date.now = () => now + 120001;
        for (const tick of h.intervals.values()) {
            tick();
        }
        expect(
            h.requests.some(x => x.request.action === "kill" && x.request.client === "first")
        ).toBe(true);
    });

    it("rebuilds a dead cached instance before displaying it", async () => {
        const h = harness();
        h.created("first");
        h.navigate("/product");
        h.created("second");
        await h.back();
        h.updates("first", [], { clientAlive: false });
        expect(h.win.document.body.textContent).toBe("");
        expect(
            h.requests.filter(item => item.request.action === "new instance").at(-1).request
        ).toMatchObject({
            action: "new instance",
            address: "/catalog",
            filter: "blue"
        });
        h.created("replacement");
        expect(h.win.document.body.textContent).toBe("replacement");
    });

    it("invalidates hidden pages on authentication change and ignores late creation", () => {
        const h = harness();
        h.navigate("/product");
        h.created("pending-catalog");
        h.created("product");
        for (const tick of h.intervals.values()) {
            tick();
        }
        h.updates("product", [{ id: "#5", action: "clear page cache" }]);
        expect(
            h.requests.some(
                x => x.request.action === "kill" && x.request.client === "pending-catalog"
            )
        ).toBe(true);
        expect(h.win.localStorage.getItem("widgetsPageCacheInvalidation")).toBeTruthy();
    });
    it("kills a late-created client after its entry was evicted", () => {
        const h = harness();
        for (let i = 0; i < 4; i++) {
            h.navigate(`/product?id=${i}`);
        }
        h.answer("new instance", { id: "late-client", serverId: "server" });
        expect(
            h.requests.some(x => x.request.action === "kill" && x.request.client === "late-client")
        ).toBe(true);
        expect(
            h.requests.some(
                x => x.request.action === "synchronize" && x.request.client === "late-client"
            )
        ).toBe(false);
    });

    it("rebuilds active and cached pages after another tab changes authentication", () => {
        const h = harness();
        h.created("first");
        h.navigate("/product");
        h.created("second");
        h.win.dispatchEvent(
            new h.win.StorageEvent("storage", {
                key: "widgetsPageCacheInvalidation",
                newValue: "other-tab"
            })
        );
        expect(
            h.requests.filter(x => x.request.action === "kill").map(x => x.request.client)
        ).toEqual(["first", "second"]);
        expect(h.requests.at(-1).request).toMatchObject({
            action: "new instance",
            address: "/product"
        });
        expect(h.win.document.body.textContent).toBe("");
    });
});
