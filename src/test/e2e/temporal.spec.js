import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

for (const locale of ["ru-RU", "en-US"]) {
    test.describe(locale, () => {
        test.use({ locale, timezoneId: "America/Los_Angeles", hasTouch: true });
        for (const width of [390, 1280]) {
            test(`native dates, minute time, range validation and keyboard at ${width}px`, async ({
                page
            }) => {
                await page.setViewportSize({ width, height: 850 });
                await page.goto("/all-widgets");
                const form = page.getByRole("form", { name: "Sample delivery", exact: true });
                const date = form.getByLabel("Delivery date", { exact: true });
                const time = form.getByLabel("Delivery time", { exact: true });
                const start = form.getByLabel("Start date", { exact: true });
                const end = form.getByLabel("End date", { exact: true });
                const submit = form.getByRole("button", { name: "Validate dates", exact: true });
                await expect(date).toHaveAttribute("type", "date");
                await expect(time).toHaveAttribute("type", "time");
                await expect(time).toHaveAttribute("step", "60");
                await expect(date).toHaveAttribute("min", "2028-01-01");
                await expect(date).toHaveValue("");
                await submit.click();
                await expect(date).toBeFocused();
                await expect(date).toHaveAccessibleDescription(/Enter a delivery date/);
                await date.fill("2028-02-29");
                await time.fill("08:59");
                await submit.click();
                await expect(time).toBeFocused();
                await expect(time).toHaveAccessibleDescription(/between 09:00 and 18:00/);
                await time.fill("09:00");
                await start.fill("2028-03-02");
                await end.fill("2028-03-01");
                await end.press("Enter");
                await expect(start).toBeFocused();
                await expect(start).toHaveAccessibleDescription(/start no later than the end/);
                await end.fill("2028-03-02");
                await end.press("Enter");
                await expect(
                    page.getByText("Accepted: 2028-02-29 / 09:00 / 2028-03-02 — 2028-03-02", {
                        exact: true
                    })
                ).toBeVisible();
                await expect(start).not.toHaveAttribute("aria-invalid", "true");
                await date.fill("2027-12-31");
                await submit.click();
                await expect(date).toBeFocused();
                await expect(date).toHaveAccessibleDescription(/in 2028/);
                await date.fill("2028-02-29");
                await start.fill("");
                await end.fill("");
                if (width === 390) await submit.tap();
                else await submit.click();
                await expect(
                    page.getByText("Accepted: 2028-02-29 / 09:00 / none", { exact: true })
                ).toBeVisible();
                await page
                    .getByRole("button", { name: "Switch locale: US / UK", exact: true })
                    .click();
                await expect(date).toHaveAttribute("lang", "en-GB");
                await expect(
                    page.getByText("Locale: en-GB; display: 29/02/2028 / 09:00", { exact: true })
                ).toBeVisible();
                await expect(date).toHaveValue("2028-02-29");
                await expect(page.getByLabel("Disabled date", { exact: true })).toBeDisabled();
                const bounds = await form.evaluate(el => ({
                    width: el.clientWidth,
                    scroll: el.scrollWidth
                }));
                expect(bounds.scroll).toBeLessThanOrEqual(bounds.width + 1);
                const audit = await new AxeBuilder({ page })
                    .include("input.widgets-temporal-field")
                    .analyze();
                expect(audit.violations).toEqual([]);
            });
        }
    });
}
