import { defineConfig } from "@playwright/test";

const baseURL = process.env.WIDGETS_E2E_BASE_URL;

if (!baseURL) {
    throw new Error("WIDGETS_E2E_BASE_URL is not set; run tests with npm run test:e2e");
}

export default defineConfig({
    testDir: "./src/test/e2e",
    projects: [
        { name: "chromium", use: { browserName: "chromium" } },
        {
            name: "webkit-suggestions",
            testMatch: [
                "suggestion-field.spec.js",
                "channel-security.spec.js",
                "connection-recovery.spec.js",
                "event-validation.spec.js",
                "event-ack.spec.js",
                "live-event-targets.spec.js",
                "labeled-choices.spec.js",
                "modal-visibility.spec.js",
                "modal-accessibility.spec.js",
                "widget-semantics.spec.js",
                "widget-reparenting.spec.js",
                "update-stream.spec.js",
                "upload-lifecycle.spec.js",
                "upload-boundary.spec.js",
                "image-processing.spec.js",
                "protocol-backpressure.spec.js"
            ],
            use: { browserName: "webkit" }
        }
    ],
    fullyParallel: false,
    workers: 1,
    timeout: 30_000,
    expect: {
        timeout: 10_000
    },
    reporter: process.env.CI
        ? [["github"], ["html", { outputFolder: "target/playwright-report", open: "never" }]]
        : "list",
    outputDir: "target/playwright-results",
    use: {
        baseURL,
        headless: true,
        trace: "retain-on-failure"
    }
});
