import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e", timeout: 45000,
  use: { baseURL: "http://127.0.0.1:5179", headless: true,
    launchOptions: { args: ["--enable-unsafe-swiftshader"] } },
  webServer: { command: "npm run dev -- --host 127.0.0.1 --port 5179 --strictPort", url: "http://127.0.0.1:5179", reuseExistingServer: false },
});
