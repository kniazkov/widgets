import fs from "node:fs";
import { readScriptGroup } from "../js/helpers/scripts.js";
import { expect, test } from "@playwright/test";

const source = readScriptGroup("widgets");
const css = fs.readFileSync(new URL("../../main/html/style.css", import.meta.url), "utf8");

for (const width of [320, 1280]) {
    test(`NoWrap keeps text and links together at ${width}px`, async ({ page }) => {
        await page.setViewportSize({ width, height: 600 });
        await page.setContent("<!doctype html><body></body>");
        await page.addStyleTag({ content: css });
        await page.addScriptTag({
            content: `${source}\nfunction initPointerEvents() {}\nfunction initFocusEvents() {}\n
            const line = widgetsLibrary.section();
            line.style.cssText += ";width:220px;font:16px monospace;align-items:baseline";
            const before = widgetsLibrary.text();
            before.textContent = "Before the group ";
            const group = widgetsLibrary["no-wrap"]();
            const prefix = widgetsLibrary.text();
            prefix.textContent = "Read our ";
            const link = widgetsLibrary.link();
            link.textContent = "privacy policy";
            link.href = "#policy";
            group.append(prefix, link);
            line.append(before, group);
            document.body.append(line);
            window.fixture = {line, before, group, prefix, link};`
        });
        const measure = () =>
            page.evaluate(() => {
                const box = el => {
                    const r = el.getBoundingClientRect();
                    return { x: r.x, y: r.y, width: r.width, height: r.height };
                };
                const range = document.createRange();
                range.selectNodeContents(fixture.link);
                return {
                    before: box(fixture.before),
                    group: box(fixture.group),
                    prefix: box(fixture.prefix),
                    link: box(fixture.link),
                    line: box(fixture.line),
                    fragments: range.getClientRects().length
                };
            });
        let bounds = await measure();
        expect(bounds.group.y).toBeGreaterThan(bounds.before.y);
        expect(bounds.link.y).toBe(bounds.prefix.y);
        expect(bounds.fragments).toBe(1);
        expect(bounds.link.x).toBeGreaterThan(bounds.prefix.x + 50);
        await page.getByRole("link", { name: "privacy policy" }).click();
        await expect(page).toHaveURL(/#policy$/);
        await page.evaluate(() => {
            fixture.line.style.width = "80px";
            fixture.link.textContent = "updated privacy policy";
        });
        bounds = await measure();
        expect(bounds.group.width).toBeGreaterThan(bounds.line.width);
        expect(bounds.link.y).toBe(bounds.prefix.y);
        expect(bounds.fragments).toBe(1);
    });
}
