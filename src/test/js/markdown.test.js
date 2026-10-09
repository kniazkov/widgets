import { readScriptGroup } from "./helpers/scripts.js";
import { JSDOM } from "jsdom";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

const source = readScriptGroup("widgets");
let dom;
let widget;

beforeEach(() => {
    dom = new JSDOM("<!doctype html>", { runScripts: "outside-only", url: "https://example.com/" });
    dom.window.eval(`${source}\nwindow.markdown = widgetsLibrary.markdown();`);
    widget = dom.window.markdown;
});
afterEach(() => dom.window.close());

function render(source) {
    widget.setText(source);
    return widget;
}

describe("Markdown documents", () => {
    it("renders headings, paragraphs, line breaks and replaces previous content", () => {
        render(
            "# Title\r\n\r\nFirst line\rnext line  \nNew line\\\nLast\n\n## Second ##\n### Third\n#### Fourth\n##### Fifth\n###### Sixth"
        );
        expect(widget.querySelector("h1").textContent).toBe("Title");
        expect(widget.querySelector("h2").textContent).toBe("Second");
        expect(widget.querySelectorAll("h1,h2,h3,h4,h5,h6")).toHaveLength(6);
        expect(widget.querySelector("p").textContent).toBe("First line next lineNew lineLast");
        expect(widget.querySelectorAll("br")).toHaveLength(2);
        render("");
        expect(widget.childNodes).toHaveLength(0);
        render("plain text");
        expect(widget.innerHTML).toBe("<p>plain text</p>");
    });

    it("renders nested lists, start numbers, continuation text, quotes and separators", () => {
        render(
            "4. Four\n5. Five\n   continuation\n   - nested\n     - deep\n6. Six\n\n> Quote\n>\n> **Second** paragraph\n\n---\n***\n___"
        );
        expect(widget.querySelector("ol").getAttribute("start")).toBe("4");
        expect(widget.querySelectorAll("ol > li")).toHaveLength(3);
        expect(widget.querySelector("ol > li:nth-child(2) > p").textContent).toBe(
            "Five continuation"
        );
        expect(widget.querySelector("ol ul ul li").textContent).toBe("deep");
        expect(widget.querySelectorAll("blockquote p")).toHaveLength(2);
        expect(widget.querySelector("blockquote strong").textContent).toBe("Second");
        expect(widget.querySelectorAll("hr")).toHaveLength(3);
    });

    it("supports emphasis, nesting, escapes, code spans and fences", () => {
        render(
            "**bold with *italic***; *italic with **bold***; ***both***; __bold__; _italic_; ~~old~~; \\*literal\\*; account_name; `**code**`; ``a ` tick``\n\n```js\n<script>literal</script>\n```\n\n~~~\nunterminated"
        );
        expect(widget.querySelector("strong em").textContent).toBe("italic");
        expect(widget.querySelector("em strong").textContent).toBe("bold");
        expect(widget.querySelector("del").textContent).toBe("old");
        expect(widget.textContent).toContain("*literal*; account_name");
        expect(widget.querySelector("code").textContent).toBe("**code**");
        expect(widget.querySelectorAll("pre")).toHaveLength(2);
        expect(widget.querySelector("pre code").textContent).toBe("<script>literal</script>");
        expect(widget.querySelectorAll("script")).toHaveLength(0);
    });

    it("allows safe links, renders HTML and unsupported images literally", () => {
        render(
            "<img src=x onerror=alert(1)> <script>alert(1)</script>\n\n[web](https://example.com) [email](mailto:a@b.com) [phone](tel:+123) [relative](/policy) [anchor](#terms) [**bold**](../about) ![image](https://example.com/x.png)"
        );
        expect(widget.querySelectorAll("a")).toHaveLength(6);
        expect(widget.querySelector("a strong").textContent).toBe("bold");
        expect(widget.querySelectorAll("img,script")).toHaveLength(0);
        expect(widget.textContent).toContain("<img src=x onerror=alert(1)>");
        expect(widget.textContent).toContain("![image](https://example.com/x.png)");
    });

    it.each([
        "javascript:evil",
        "JaVaScRiPt:evil",
        "data:text/html,evil",
        "vbscript:evil",
        "file:///tmp/x",
        "java\tscript:evil",
        "java\nscript:evil",
        "javascript&#58;evil"
    ])("never activates dangerous or obfuscated URL %s", href => {
        render(`[bad](${href})`);
        for (const link of widget.querySelectorAll("a")) {
            expect(["https:", "http:", "mailto:", "tel:"]).toContain(link.protocol);
        }
        expect(widget.querySelectorAll("script,img,iframe")).toHaveLength(0);
    });

    it("preserves incomplete syntax and bounds deeply nested content", () => {
        render("Unclosed **bold, `code and [link](\n\n" + "> ".repeat(1000) + "deep");
        expect(widget.textContent).toContain("Unclosed **bold, `code and [link](");
        expect(widget.textContent).toContain("deep");
        expect(widget.querySelectorAll("blockquote").length).toBeLessThanOrEqual(32);
    });
});
