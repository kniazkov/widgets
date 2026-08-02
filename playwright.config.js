import { defineConfig } from "@playwright/test";

const baseURL = process.env.WIDGETS_E2E_BASE_URL;

if (!baseURL) {
    throw new Error("WIDGETS_E2E_BASE_URL is not set; run tests with npm run test:e2e");
}

export default defineConfig({
    testDir: "./src/test/e2e",
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
