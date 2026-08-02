import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const source = fs.readFileSync(
    new URL("../../main/html/scripts/lib.js", import.meta.url),
    "utf8"
);

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
