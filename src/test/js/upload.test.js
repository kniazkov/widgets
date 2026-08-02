import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it, vi } from "vitest";

const optionsSource = fs.readFileSync(
    new URL("../../main/html/scripts/options.js", import.meta.url),
    "utf8"
);
const widgetsSource = fs.readFileSync(
    new URL("../../main/html/scripts/widgets.js", import.meta.url),
    "utf8"
);

const chunkSize = 64 * 1024;
let dom;

afterEach(() => {
    vi.useRealTimers();
    dom?.window.close();
});

function loadBrowserCode() {
    dom = new JSDOM("<!doctype html>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.log = vi.fn();
    dom.window.sendSynchronizeRequest = vi.fn();
    dom.window.eval(`let clientId = "#1";\n${optionsSource}\n${widgetsSource}`);
}

describe("file upload", () => {
    it("reads and sends only one binary slice at a time without an empty boundary chunk", async () => {
        vi.useFakeTimers();
        loadBrowserCode();
        const requests = [];
        const acknowledgements = [];
        dom.window.sendRequest = (data, callback, method, files) => {
            requests.push({ data, method, files });
            acknowledgements.push(callback);
        };
        dom.window.FileReader = class {
            constructor() {
                throw new Error("The complete file must not be read into memory");
            }
        };

        const file = new dom.window.File([new Uint8Array(chunkSize * 2)], "boundary.bin", {
            type: "application/octet-stream"
        });
        const originalSlice = file.slice.bind(file);
        const slices = [];
        file.slice = (start, end, type) => {
            slices.push([start, end]);
            return originalSlice(start, end, type);
        };
        const widget = { _id: "#7", _files: [] };

        expect(() => dom.window.loadFile(widget, file)).not.toThrow();

        expect(slices).toEqual([[0, chunkSize]]);
        expect(requests).toHaveLength(1);

        acknowledgements.shift()('{"result":true}');
        await vi.runOnlyPendingTimersAsync();

        expect(slices).toEqual([
            [0, chunkSize],
            [chunkSize, chunkSize * 2]
        ]);
        expect(requests).toHaveLength(2);

        acknowledgements.shift()('{"result":true}');
        await vi.runOnlyPendingTimersAsync();

        expect(slices).toEqual([
            [0, chunkSize],
            [chunkSize, chunkSize * 2]
        ]);
        expect(requests).toHaveLength(2);
        expect(requests.map(request => request.data.chunkIndex)).toEqual([0, 1]);
        expect(requests.every(request => request.data.totalChunks === 2)).toBe(true);
        expect(requests.every(request => request.data.content === undefined)).toBe(true);
        expect(requests.every(request => request.method === "post")).toBe(true);
        expect(requests.map(request => request.files[0].size)).toEqual([chunkSize, chunkSize]);
        expect(dom.window.sendSynchronizeRequest).toHaveBeenCalledTimes(2);
        expect(widget._files).toEqual([]);
    });
});
