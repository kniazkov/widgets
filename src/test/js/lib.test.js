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

    it("reports a failed transport exactly once", () => {
        class FailingXmlHttpRequest {
            open() {}

            send() {
                this.readyState = 4;
                this.status = 500;
                this.onreadystatechange();
                this.onerror();
            }
        }

        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.XMLHttpRequest = FailingXmlHttpRequest;
        dom.window.eval(source);
        const responses = [];

        dom.window.sendRequest({ action: "upload" }, response => responses.push(response));

        expect(responses).toEqual([null]);
    });
});
