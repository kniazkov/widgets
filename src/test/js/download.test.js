import { readScript } from "./helpers/scripts.js";
import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it, vi } from "vitest";

let dom;
afterEach(() => dom?.window.close());

function setup() {
    dom = new JSDOM("<!doctype html><body></body>", { runScripts: "outside-only" });
    const window = dom.window;
    window.URL.createObjectURL = vi.fn(() => "blob:private-file");
    window.URL.revokeObjectURL = vi.fn();
    const timers = [];
    window.setTimeout = callback => timers.push(callback);
    const clicked = [];
    window.HTMLAnchorElement.prototype.click = function () {
        clicked.push({ href: this.href, name: this.download, connected: this.isConnected });
    };
    window.eval(readScript("client/lifecycle.js"));
    return { window, clicked, timers };
}

describe("скачивание файла", () => {
    it("создаёт временный Blob, скачивает и освобождает адрес", () => {
        const { window, clicked, timers } = setup();
        expect(
            window.downloadFile({
                name: "cdek-123.pdf",
                contentType: "application/pdf",
                content: "JVBERi0="
            })
        ).toBe(true);
        expect(clicked).toEqual([
            { href: "blob:private-file", name: "cdek-123.pdf", connected: true }
        ]);
        const blob = window.URL.createObjectURL.mock.calls[0][0];
        expect(blob.type).toBe("application/pdf");
        expect(blob.size).toBe(5);
        expect(window.document.querySelector("a")).toBeNull();
        timers[0]();
        expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:private-file");
    });

    it("не создаёт файл для некорректного имени, MIME-типа или Base64", () => {
        const { window } = setup();
        for (const override of [
            { name: "../file" },
            { contentType: "bad" },
            { content: "%" },
            { content: "" }
        ]) {
            expect(
                window.downloadFile({
                    name: "file.pdf",
                    contentType: "application/pdf",
                    content: "JVBERi0=",
                    ...override
                })
            ).toBe(false);
        }
        expect(window.URL.createObjectURL).not.toHaveBeenCalled();
    });
});
