import fs from "node:fs";
import { expect, test } from "@playwright/test";

const source = fs.readFileSync(
    new URL("../../main/html/scripts/widgets.js", import.meta.url),
    "utf8"
);
const css = fs.readFileSync(new URL("../../main/html/style.css", import.meta.url), "utf8");

test("content-sized overlays follow text, fonts and viewport changes without intrinsic feedback", async ({
    page
}) => {
    await page.setContent("<!doctype html><body></body>");
    await page.addStyleTag({ content: css });
    await page.addScriptTag({
        content: `${source}\nfunction initPointerEvents() {}\n
        const stack = widgetsLibrary["overlay stack"]();
        const text = document.createElement("span");
        text.textContent = "2390.00";
        const overlay = document.createElement("div");
        const image = document.createElement("img");
        image.src = "data:image/svg+xml," + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="100" height="24" viewBox="0 0 100 24" preserveAspectRatio="none"><path d="M0 0L100 24M0 24L100 0" stroke="red"/></svg>');
        image.style.cssText = "width:100%;height:100%;display:block";
        overlay.appendChild(image);
        stack.append(text, overlay);
        [...stack.children].forEach(child => stack._layoutChild(child));
        widgets.stack = stack;
        setFitToFirstChild({widget:"stack", "fit to first child":true});
        document.body.appendChild(stack);
        window.fixture = {stack, text, overlay, image};
    `
    });
    for (const width of [320, 1280, 390]) {
        await page.setViewportSize({ width, height: 600 });
        for (const font of [12, 40, 18]) {
            for (const value of ["9", "123456789.00", "2390.00"]) {
                await page.evaluate(
                    ({ font, value }) => {
                        fixture.text.style.fontSize = font + "px";
                        fixture.text.textContent = value;
                    },
                    { font, value }
                );
                const sizes = await page.evaluate(() => {
                    const rect = el => {
                        const r = el.getBoundingClientRect();
                        return [r.x, r.y, r.width, r.height];
                    };
                    return [fixture.stack, fixture.text, fixture.overlay, fixture.image].map(rect);
                });
                for (const size of sizes.slice(1)) {
                    size.forEach((number, index) =>
                        expect(Math.abs(number - sizes[0][index])).toBeLessThan(1)
                    );
                }
            }
        }
    }
    await page.evaluate(() => {
        fixture.stack.removeChild(fixture.text);
        const replacement = document.createElement("span");
        replacement.textContent = "New first layer";
        fixture.stack.prepend(replacement);
        fixture.text = replacement;
    });
    expect(await page.evaluate(() => getComputedStyle(fixture.text).position)).toBe("static");
    expect(await page.evaluate(() => getComputedStyle(fixture.overlay).position)).toBe("absolute");
    await page.evaluate(() => setFitToFirstChild({ widget: "stack", "fit to first child": false }));
    expect(await page.evaluate(() => getComputedStyle(fixture.overlay).position)).toBe("static");
});
