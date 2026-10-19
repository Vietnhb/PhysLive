import { test, expect, type Page } from "@playwright/test";

async function staffSession(page: Page, permissions: string[]) {
  await page.addInitScript(() => {
    localStorage.setItem("token", "header." + btoa(JSON.stringify({ exp: 4102444800 })) + ".signature");
  });
  await page.route("http://127.0.0.1:5179/api/**", async route => {
    const path = new URL(route.request().url()).pathname;
    const user = { id: 5, email: "staff@example.test", fullName: "Staff test", role: "STAFF",
      schoolId: "11111111-1111-4111-8111-111111111111", permissions };
    let json: unknown = [];
    if (path === "/api/user/me") json = user;
    else if (path.endsWith("/license")) json = { canPerformWriteOperations: true, showRenewalBanner: false };
    else if (path === "/api/simulation/understand") json = {
      sessionId: "test-session", stage: "CLARIFY", description: "A moving car",
      question: "Xe chuyển động theo hướng nào?", choices: ["Sang phải", "Sang trái"],
    };
    else if (path.includes("/assignments")) json = { content: [], totalElements: 0, totalPages: 0 };
    await route.fulfill({ json });
  });
}

test("teaching Staff can enter a description and receive a clarification on a phone", async ({ page }, info) => {
  await page.setViewportSize({ width: 412, height: 915 });
  await staffSession(page, ["TEACH"]);
  await page.goto("/workspace");
  await page.locator("#simulation-input").fill("A moving car");
  await page.getByRole("button", { name: "Nhận diện đề bài", exact: true }).click();
  await expect(page.getByText("Xe chuyển động theo hướng nào?", { exact: true })).toBeVisible();
  await expect.poll(() => page.locator(".brand-mark__logo").evaluate(image => (image as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
  await page.screenshot({ path: info.outputPath("staff-mobile-clarification.png"), fullPage: true });
});

test("a department head without TEACH is redirected away from creation", async ({ page }) => {
  await staffSession(page, ["DEPARTMENT_HEAD_PHYSICS"]);
  await page.goto("/workspace");
  await expect(page).toHaveURL(/\/department$/);
  await expect(page.locator("#simulation-input")).toHaveCount(0);
});
