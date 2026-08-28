import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const source = fs.readFileSync(new URL("../../main/html/scripts/lib.js", import.meta.url), "utf8");

let dom;

afterEach(() => {
    dom?.window.close();
});

describe("escapeHtml", () => {
    it("escapes every HTML-sensitive character", () => {
        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.eval(source);

        expect(dom.window.escapeHtml(`<script data-x="a&b">'x'</script>`)).toBe(
            "&lt;script data-x=&quot;a&amp;b&quot;&gt;&#039;x&#039;&lt;/script&gt;"
        );
    });
});

describe("sendRequest", () => {
    it("does not abort an in-flight request when another request starts", () => {
        const requests = [];

        class MockXmlHttpRequest {
            constructor() {
                this.readyState = 0;
                this.aborted = false;
                requests.push(this);
            }

            open() {
                if (this.readyState > 0 && this.readyState < 4) {
                    this.aborted = true;
                }
                this.readyState = 1;
            }

            send() {}
        }

        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.XMLHttpRequest = MockXmlHttpRequest;
        dom.window.eval(source);

        dom.window.sendRequest({ action: "first" });
        dom.window.sendRequest({ action: "second" });

        expect(requests).toHaveLength(2);
        expect(requests.every(request => !request.aborted)).toBe(true);
    });

    it("sends binary data as an octet-stream body with encoded query fields", () => {
        const requests = [];

        class MockXmlHttpRequest {
            constructor() {
                this.headers = {};
                requests.push(this);
            }

            open(method, url) {
                this.method = method;
                this.url = url;
            }

            setRequestHeader(name, value) {
                this.headers[name] = value;
            }

            send(body) {
                this.body = body;
            }
        }

        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://example.test:8080/"
        });
        dom.window.XMLHttpRequest = MockXmlHttpRequest;
        dom.window.eval(source);
        const chunk = new dom.window.Blob([new Uint8Array([1, 2, 3])]);

        dom.window.sendBinaryRequest(
            { action: "upload chunk", client: "#1", metadata: { index: 2 } },
            null,
            chunk
        );

        expect(requests).toHaveLength(1);
        expect(requests[0].method).toBe("POST");
        expect(requests[0].url).toBe(
            "http://example.test:8080?action=upload%20chunk&client=%231&metadata=%7B%22index%22%3A2%7D"
        );
        expect(requests[0].headers).toEqual({
            "Content-Type": "application/octet-stream"
        });
        expect(requests[0].body).toBe(chunk);
    });
});
