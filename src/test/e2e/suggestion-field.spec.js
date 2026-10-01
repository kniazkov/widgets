import { test, expect } from "@playwright/test";

async function expectAnchored(field, side = "below") {
    await expect
        .poll(() =>
            field.evaluate((input, side) => {
                const list = document.querySelector('[role="listbox"]');
                if (!list || getComputedStyle(list).visibility === "hidden") return false;
                const anchor = input.getBoundingClientRect();
                const popup = list.getBoundingClientRect();
                const viewport = window.visualViewport;
                const top = viewport?.offsetTop ?? 0;
                const left = viewport?.offsetLeft ?? 0;
                const bottom = top + (viewport?.height ?? innerHeight);
                const right = left + (viewport?.width ?? innerWidth);
                const gap =
                    side === "below" ? popup.top - anchor.bottom : anchor.top - popup.bottom;
                return (
                    Math.abs(gap - 4) <= 1 &&
                    popup.top >= top + 3 &&
                    popup.bottom <= bottom - 3 &&
                    popup.left >= left + 3 &&
                    popup.right <= right - 3 &&
                    popup.height > 0
                );
            }, side)
        )
        .toBe(true);
}

test("keyboard selection and saved values travel through Java models", async ({ page }) => {
    await page.goto("/suggestions");
    const first = page.getByRole("combobox").nth(0);
    const next = page.getByRole("combobox").nth(1);
    await first.focus();
    await expect(page.getByRole("option")).toHaveCount(3);
    await first.fill("РОД");
    await expect(page.getByRole("option")).toHaveText(["Латунь с родиевым покрытием"]);
    await first.press("ArrowDown");
    await first.press("Enter");
    await expect(first).toHaveValue("Латунь с родиевым покрытием");
    await expect(
        page.getByText("Value: Латунь с родиевым покрытием", { exact: true })
    ).toBeVisible();
    await expect(page.getByRole("listbox")).toHaveCount(0);
    await first.fill("Медь");
    await next.focus();
    await expect(page.getByRole("option")).toHaveCount(3);
    await page.getByRole("button", { name: "Save value" }).click();
    await next.focus();
    await expect(page.getByRole("option", { name: "Медь", exact: true })).toBeVisible();
    await next.press("Escape");
    await expect(next).toHaveValue("");
    await expect(page.getByRole("listbox")).toHaveCount(0);
});

test("source model changes update both fields without overwriting entered text", async ({
    page
}) => {
    await page.goto("/suggestions");
    const first = page.getByRole("combobox").nth(0);
    const next = page.getByRole("combobox").nth(1);
    await first.fill("Draft");
    await page.getByRole("button", { name: "Rename source value" }).click();
    await expect(first).toHaveValue("Draft");
    await next.focus();
    await expect(
        page.getByRole("option", { name: "Родированная латунь", exact: true })
    ).toBeVisible();
    await expect(
        page.getByRole("option", { name: "Латунь с родиевым покрытием", exact: true })
    ).toHaveCount(0);
    await first.fill("");
    await expect(
        page.getByRole("option", { name: "Родированная латунь", exact: true })
    ).toBeVisible();
});

test.describe("mobile suggestions", () => {
    test.use({
        viewport: { width: 390, height: 700 },
        hasTouch: true,
        isMobile: true
    });

    test("follows late layout movement without viewport events and still accepts a tap", async ({
        page
    }) => {
        await page.goto("/suggestions");
        const field = page.getByRole("combobox").first();
        await field.evaluate(input => {
            input.style.width = "220px";
        });
        await field.tap();
        await expectAnchored(field);
        await field.evaluate(() => {
            window.__openSuggestions = document.querySelector('[role="listbox"]');
        });
        for (const [x, y] of [
            [30, 200],
            [10, 80],
            [50, 300]
        ]) {
            await field.evaluate(
                (input, [x, y]) => {
                    input.style.transform = `translate(${x}px, ${y}px)`;
                },
                [x, y]
            );
            await expectAnchored(field);
        }
        expect(
            await page.evaluate(
                () => window.__openSuggestions === document.querySelector('[role="listbox"]')
            )
        ).toBe(true);
        await page.getByRole("option", { name: "Серебро", exact: true }).tap();
        await expect(field).toHaveValue("Серебро");
        await expect(field).toBeFocused();
        await expect(page.getByRole("listbox")).toHaveCount(0);
    });

    test("keeps suggestions inside a keyboard-sized, panned visual viewport", async ({ page }) => {
        await page.goto("/suggestions");
        const field = page.getByRole("combobox").first();
        // Desktop WebKit does not open an iPhone keyboard. Supply its viewport
        // geometry while keeping real DOM layout and top-layer popup rendering.
        await field.evaluate(input => {
            input.style.position = "fixed";
            input.style.top = "300px";
            input.style.left = "50px";
            input.style.width = "260px";
            const viewport = new EventTarget();
            Object.assign(viewport, { offsetTop: 100, offsetLeft: 30, width: 320, height: 280 });
            Object.defineProperty(window, "visualViewport", {
                configurable: true,
                value: viewport
            });
        });
        await field.tap();
        await expectAnchored(field, "above");
        await page.evaluate(() => {
            Object.assign(visualViewport, { offsetTop: 220, height: 140, width: 220 });
            visualViewport.dispatchEvent(new Event("resize"));
        });
        await expectAnchored(field, "above");
        await field.evaluate(input => {
            input.style.top = "225px";
        });
        await expectAnchored(field);
        await field.evaluate(input => {
            input.style.top = "450px";
        });
        await expect(page.getByRole("listbox", { includeHidden: true })).toBeHidden();
        await field.evaluate(input => {
            input.style.top = "225px";
        });
        await expectAnchored(field);
        await field.press("Escape");
        await expect(page.getByRole("listbox")).toHaveCount(0);
    });

    test("follows scrolling inside a form", async ({ page }) => {
        await page.goto("/suggestions");
        const field = page.getByRole("combobox").first();
        await field.evaluate(input => {
            const scroller = document.createElement("div");
            scroller.style.cssText = "height: 230px; overflow: auto";
            input.before(scroller);
            const before = document.createElement("div");
            before.style.height = "150px";
            const after = document.createElement("div");
            after.style.height = "500px";
            scroller.append(before, input, after);
            input.style.width = "220px";
            input.focus({ preventScroll: true });
        });
        await expectAnchored(field);
        await field.evaluate(input => {
            input.parentElement.scrollTop = 100;
        });
        await expectAnchored(field);
    });

    test("a blur during a native tap still selects once and keeps the caret", async ({ page }) => {
        await page.goto("/suggestions");
        const field = page.getByRole("combobox").first();
        await field.tap();
        await field.evaluate(input => {
            window.__suggestionInputs = 0;
            input.addEventListener("input", () => window.__suggestionInputs++);
            document.addEventListener(
                "touchstart",
                event => {
                    if (event.target.closest('[role="option"]')) input.blur();
                },
                { once: true }
            );
        });
        const value = "Латунь с родиевым покрытием";
        await page.getByRole("option", { name: value, exact: true }).tap();
        await expect(field).toHaveValue(value);
        await expect(field).toBeFocused();
        await expect(page.getByText("Value: " + value, { exact: true })).toBeVisible();
        await expect(page.getByRole("listbox")).toHaveCount(0);
        expect(await page.evaluate(() => window.__suggestionInputs)).toBe(1);
        expect(await field.evaluate(input => input.selectionStart)).toBe(value.length);
        await page.keyboard.type("!");
        await expect(field).toHaveValue(value + "!");
    });

    test("tap selects editable text and an outside tap dismisses", async ({ page }) => {
        await page.goto("/suggestions");
        const first = page.getByRole("combobox").first();
        await first.tap();
        await page.getByRole("option", { name: "Серебро", exact: true }).tap();
        await expect(first).toHaveValue("Серебро");
        await expect(page.getByText("Value: Серебро", { exact: true })).toBeVisible();
        await expect(page.getByRole("listbox")).toHaveCount(0);
        await first.fill("Серебро 925");
        await expect(page.getByText("Value: Серебро 925", { exact: true })).toBeVisible();
        await first.fill("");
        await expect(page.getByRole("option")).toHaveCount(3);
        await page.touchscreen.tap(380, 600);
        await expect(page.getByRole("listbox")).toHaveCount(0);
        expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
            390
        );
    });
});

for (const touch of [false, true]) {
    test.describe(touch ? "touch token suggestions" : "keyboard token suggestions", () => {
        test.use({ viewport: { width: 390, height: 700 }, hasTouch: touch, isMobile: touch });
        test("replaces only the current color and updates its Java text model", async ({
            page
        }) => {
            await page.goto("/suggestions");
            const colors = page.getByRole("combobox").nth(2);
            await colors.fill("Black, Wh, Blue");
            await colors.evaluate(input => {
                input.setSelectionRange(9, 9);
                input.click();
            });
            await expect(page.getByRole("option")).toHaveText(["White"]);
            if (touch) await page.getByRole("option", { name: "White", exact: true }).tap();
            else {
                await colors.press("ArrowDown");
                await colors.press("Enter");
            }
            await expect(colors).toHaveValue("Black, White, Blue");
            await expect(
                page.getByText("Colors: Black, White, Blue", { exact: true })
            ).toBeVisible();
            await expect(page.getByRole("listbox")).toHaveCount(0);
            await colors.fill("White, White, Black");
            await colors.press("Tab");
            await expect(colors).toHaveValue("White, Black");
            await expect(page.getByText("Colors: White, Black", { exact: true })).toBeVisible();
            await colors.fill("White, ");
            await expect(page.getByRole("option", { name: "White", exact: true })).toHaveCount(0);
        });
    });
}

for (const touch of [false, true]) {
    test.describe(touch ? "touch caret" : "mouse caret", () => {
        test.use({ viewport: { width: 390, height: 700 }, hasTouch: touch, isMobile: touch });
        test("continues typing after a long selected suggestion", async ({ page }) => {
            await page.goto("/suggestions");
            const field = page.getByRole("combobox").first();
            await field.evaluate(input => {
                input.style.width = "160px";
            });
            if (touch) await field.tap();
            else await field.click();
            const value = "Латунь с родиевым покрытием";
            const option = page.getByRole("option", { name: value, exact: true });
            if (touch) await option.tap();
            else await option.click();
            await expect(field).toBeFocused();
            await expect(field).toHaveValue(value);
            await expect(page.getByRole("listbox")).toHaveCount(0);
            await expect
                .poll(() => field.evaluate(input => input.selectionStart))
                .toBe(value.length);
            await expect.poll(() => field.evaluate(input => input.scrollLeft)).toBeGreaterThan(0);
            await page.keyboard.type(", Silver");
            await expect(field).toHaveValue(value + ", Silver");
        });
    });
}
