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
    dom = new JSDOM("<!doctype html>", {
        runScripts: "outside-only",
        url: "http://localhost/"
    });
    dom.window.eval(`${optionsSource}\n${librarySource}\n
        configureUploadProtocol(4 * 1024, 128 * 1024 * 1024);
        window.__uploadEvents = [];
        let clientId = "#1";
        function createEvent(widget, type, data) {
            window.__uploadEvents.push({ widget, type, data });
        }
        function sendSynchronizeRequest(callback) {
            callback(true);
        }
        ${widgetsSource}`);

    const requests = [];
    dom.window.sendRequest = (query, callback, method, files) => {
        requests.push({ query, callback, method, files });
    };
    return {
        requests,
        events: dom.window.__uploadEvents,
        widget: { _id: "#9" }
    };
}

function file(name, size) {
    const content = new Uint8Array(size);
    for (let index = 0; index < content.length; index++) {
        content[index] = index % 251;
    }
    return new dom.window.File([content], name, {
        type: "application/octet-stream"
    });
}

describe("binary upload scheduler", () => {
    it("registers every file immediately and round-robins five active files", () => {
        const harness = createHarness();
        dom.window.FileReader = class {
            constructor() {
                throw new Error("FileReader must not be used by binary uploads");
            }
        };
        const files = Array.from({ length: 7 }, (_, index) =>
            file(`file-${index + 1}.bin`, 4 * 1024 + 1)
        );

        dom.window.loadFiles(harness.widget, files);

        expect(harness.events).toHaveLength(7);
        expect(harness.events.every(event => event.type === "upload")).toBe(true);
        expect(harness.events.map(event => event.data.fileId)).toEqual([1, 2, 3, 4, 5, 6, 7]);
        expect(harness.events.every(event => !("content" in event.data))).toBe(true);
        expect(harness.requests).toHaveLength(1);

        const sequence = [];
        for (let index = 0; index < 10; index++) {
            const request = harness.requests[index];
            sequence.push(request.query.fileId);
            expect(request.method).toBe("post");
            expect(request.files[0].data).toBeInstanceOf(dom.window.Blob);
            request.callback(
                JSON.stringify({
                    result: true,
                    nextChunk: request.query.chunkIndex + 1,
                    complete: request.query.chunkIndex == 1
                })
            );
        }

        expect(sequence).toEqual([1, 2, 3, 4, 5, 1, 2, 3, 4, 5]);
        expect(harness.requests[10].query.fileId).toBe(6);
    });

    it("retries an unacknowledged chunk after giving other files a turn", async () => {
        const harness = createHarness();
        dom.window.loadFiles(harness.widget, [file("first.bin", 3), file("second.bin", 3)]);

        const firstAttempt = harness.requests[0];
        expect(firstAttempt.query.fileId).toBe(1);
        expect(firstAttempt.query.chunkIndex).toBe(0);
        firstAttempt.callback(null);

        await new Promise(resolve => dom.window.setTimeout(resolve, 300));

        const secondFile = harness.requests[1];
        expect(secondFile.query.fileId).toBe(2);
        secondFile.callback(JSON.stringify({ result: true, nextChunk: 1, complete: true }));

        const retry = harness.requests[2];
        expect(retry.query.fileId).toBe(1);
        expect(retry.query.chunkIndex).toBe(0);
        expect(retry.files[0].data.size).toBe(3);
    });
});
