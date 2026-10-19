import { test, expect } from "@playwright/test";
import { pathToFileURL } from "node:url";

test("downloaded replay runs without a server and keeps the scene sandboxed", async ({ page, context }, info) => {
  page.on("console", message => { if (message.type() === "error") console.error(message.text()); });
  page.on("pageerror", error => console.error(error.message));
  await page.goto("/tests/offline-replay/index.html");
  const downloading = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export" }).click();
  const download = await downloading;
  const file = info.outputPath("physlive-replay.html");
  await download.saveAs(file);
  await context.setOffline(true);
  await page.goto(pathToFileURL(file).href);
  await expect(page.locator("#description")).toHaveText("</script text must remain text>");
  await expect(page.locator("iframe")).toHaveAttribute("sandbox", "allow-scripts");
  await expect(page.locator("#time")).not.toHaveText("0 s", { timeout: 30000 });
  await expect(page.locator("#error")).toBeEmpty();
  await page.locator("#seek").fill("1");
  await expect(page.locator("#values")).toContainText('"x"');
  await page.screenshot({ path: info.outputPath("offline-replay.png") });
});
