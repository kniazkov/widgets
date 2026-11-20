import { expect, test } from "@playwright/test";

function controls(page) {
    const form = page.getByRole("form", { name: "Request with sample data" });
    return {
        form,
        title: form.getByRole("textbox", {
            name: "Full sample item name for the demonstration request"
        }),
        confirm: form.getByRole("textbox", {
            name: "Repeat the same name to test validation across two fields"
        }),
        reference: form.getByRole("textbox", { name: "Sample number" }),
        submit: form.getByRole("button", { name: "Submit request" }),
        status: form.getByRole("status")
    };
}

for (const width of [320, 390, 1280]) {
    test(`Russian form keeps labels, help, required and first-error focus at ${width}px`, async ({
        page
    }) => {
        await page.setViewportSize({ width, height: 760 });
        await page.goto("/all-widgets");
        const { title, confirm, reference, submit, status } = controls(page);
        await expect(title).toHaveAccessibleDescription(/Maximum 40 characters/);
        await expect(title).toHaveAttribute("maxlength", "40");
        await expect(title).toHaveAttribute("autocomplete", "off");
        await expect(title).toHaveAttribute("name", "title");
        await expect(title).toHaveAttribute("required", "");
        await expect(reference).toHaveAttribute("readonly", "");
        await expect(reference).toHaveValue("DEMO-20");
        await page
            .locator("label")
            .filter({ hasText: "Full sample item name for the demonstration request" })
            .click();
        await expect(title).toBeFocused();
        await title.press("Tab");
        await expect(confirm).toBeFocused();
        await confirm.press("Enter");
        await expect(status).toHaveText("Check the highlighted fields.");
        await expect(title).toBeFocused();
        await expect(title).toHaveAttribute("aria-invalid", "true");
        await expect(title).toHaveAccessibleDescription(
            /Maximum 40 characters.*Complete this field/
        );
        expect(
            await title.evaluate(el => el.closest(".widgets-form").scrollWidth)
        ).toBeLessThanOrEqual(
            (await title.locator("xpath=ancestor::*[@role='form']").boundingBox()).width + 1
        );
        await title.fill("Paper");
        await confirm.fill("Blue");
        await submit.click();
        await expect(confirm).toBeFocused();
        await expect(confirm).toHaveAccessibleDescription("Names in both fields must match.");
    });
}

test("IME confirmation does not submit; async refusal retains entered text and permits retry", async ({
    page
}) => {
    await page.setViewportSize({ width: 390, height: 760 });
    await page.goto("/all-widgets");
    const { form, title, confirm, submit, status } = controls(page);
    const attempts = page.getByText(/^Sample submissions: /);
    await title.fill("Taken");
    await confirm.fill("Taken");
    await confirm.evaluate(el => {
        el.dispatchEvent(new CompositionEvent("compositionstart"));
        el.dispatchEvent(
            new KeyboardEvent("keydown", { key: "Enter", isComposing: true, bubbles: true })
        );
        el.dispatchEvent(new CompositionEvent("compositionend"));
        el.dispatchEvent(new KeyboardEvent("keyup", { key: "Enter", bubbles: true }));
    });
    await expect(attempts).toHaveText("Sample submissions: 0");
    await confirm.press("Enter");
    await expect(form).toHaveAttribute("aria-busy", "true");
    await expect(submit).toBeDisabled();
    await expect(status).toHaveText("Validating request…");
    // A second deliberate click and Enter are not a transport retry of the first action.
    await submit.scrollIntoViewIfNeeded();
    const box = await submit.boundingBox();
    await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
    await confirm.press("Enter");
    await expect(attempts).toHaveText("Sample submissions: 1");
    await expect(status).toHaveText("The server rejected the sample request.", { timeout: 10000 });
    await expect(title).toHaveValue("Taken");
    await expect(confirm).toHaveValue("Taken");
    await expect(title).toBeFocused();
    await expect(title).toHaveAccessibleDescription(/This sample is already reserved/);
    await expect(submit).toBeEnabled();
    await title.fill("Paper");
    await confirm.fill("Paper");
    await submit.click();
    await expect(status).toHaveText("Sample request accepted.", { timeout: 10000 });
    await expect(attempts).toHaveText("Sample submissions: 2");
    await expect(title).not.toHaveAttribute("aria-invalid", "true");
});

test.describe("touch form controls", () => {
    test.use({ viewport: { width: 390, height: 760 }, hasTouch: true });
    test("tap the native label and submit button", async ({ page }) => {
        await page.goto("/all-widgets");
        const { title, confirm, submit, status } = controls(page);
        await page
            .locator("label")
            .filter({ hasText: "Full sample item name for the demonstration request" })
            .tap();
        await expect(title).toBeFocused();
        await title.fill("Paper");
        await confirm.fill("Paper");
        await submit.tap();
        await expect(status).toHaveText("Sample request accepted.", { timeout: 10000 });
    });
});
