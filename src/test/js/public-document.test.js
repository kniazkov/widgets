import { JSDOM } from "jsdom";
import { readScript } from "./helpers/scripts.js";
import { it, expect } from "vitest";

it("builds literal DOM and rejects executable URLs without HTML parsing", () => {
    const dom = new JSDOM("<!doctype html><body></body>", {
        runScripts: "outside-only",
        url: "https://example.com"
    });
    try {
        const win = dom.window;
        win.eval(readScript("shared/document.js"));
        const article = win.renderPublicDocument({
            blocks: [
                { tag: "h1", text: '<script>alert("x")</script>' },
                { tag: "a", text: "<b>Link</b>", url: "/product?a=1&b=2" },
                {
                    tag: "img",
                    text: '" onerror="alert(1)',
                    url: "/image.png",
                    width: 240,
                    height: 120
                }
            ]
        });
        expect(article.querySelector("script, b")).toBeNull();
        expect(article.querySelector("a").textContent).toBe("<b>Link</b>");
        expect(article.querySelector("img").hasAttribute("onerror")).toBe(false);
        expect(() =>
            win.renderPublicDocument({ blocks: [{ tag: "script", text: "x" }] })
        ).toThrow();
        expect(() =>
            win.renderPublicDocument({
                blocks: [{ tag: "a", text: "x", url: "javascript:alert(1)" }]
            })
        ).toThrow();
        win.applyDocumentMetadata({
            title: "Public",
            canonical: "https://example.com/product",
            "og:title": "<img src=x>"
        });
        expect(win.document.head.querySelector("img")).toBeNull();
        win.applyDocumentMetadata({ title: "Private" });
        expect(
            win.document.head.querySelector('link[rel="canonical"], meta[property^="og:"]')
        ).toBeNull();
    } finally {
        dom.window.close();
    }
});
