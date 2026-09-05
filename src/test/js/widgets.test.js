import fs from "node:fs";

import { JSDOM } from "jsdom";
import { afterEach, describe, expect, it } from "vitest";

const source = fs.readFileSync(
    new URL("../../main/html/scripts/widgets.js", import.meta.url),
    "utf8"
);

let dom;

afterEach(() => {
    dom?.window.close();
});

describe("text input alignment", () => {
    it("applies every horizontal alignment supported by the protocol", () => {
        dom = new JSDOM("<!doctype html>", {
            runScripts: "outside-only",
            url: "http://localhost/"
        });
        dom.window.eval(`${source}\nwindow.__initTextAlignment = initTextAlignment;`);

        const input = dom.window.document.createElement("input");
        dom.window.__initTextAlignment(input);

        input._setHorzAlignment("center");
        expect(input.style.textAlign).toBe("center");
        input._setHorzAlignment("right");
        expect(input.style.textAlign).toBe("right");
        input._setHorzAlignment("justify");
        expect(input.style.textAlign).toBe("justify");
        input._setHorzAlignment("left");
        expect(input.style.textAlign).toBe("left");
    });
});
