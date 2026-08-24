import { test, expect } from "@playwright/test";

/**
 * These specs deliberately need no backend and no authenticated session —
 * they exercise middleware.ts's route-guard logic (unauthenticated visitors
 * get bounced to /login with a redirect param preserved) and the two other
 * public auth pages, the same "no live app required" pattern login.spec.ts
 * already uses for its redirect-guard test.
 */
test.describe("Route guards (unauthenticated visitor)", () => {
  test("redirects from a student route to /login and preserves the redirect target", async ({ page }) => {
    await page.goto("/student/dashboard");
    await expect(page).toHaveURL(/\/login\?redirect=%2Fstudent%2Fdashboard/);
  });

  test("redirects from an admin route to /login and preserves the redirect target", async ({ page }) => {
    await page.goto("/admin/students");
    await expect(page).toHaveURL(/\/login\?redirect=%2Fadmin%2Fstudents/);
  });

  test("redirects from the AI Assistant route to /login", async ({ page }) => {
    await page.goto("/student/chatbot");
    await expect(page).toHaveURL(/\/login/);
  });

  test("does not redirect away from the public forgot-password page", async ({ page }) => {
    await page.goto("/forgot-password");
    await expect(page).toHaveURL(/\/forgot-password/);
    await expect(page.getByRole("heading", { name: "Reset your password" })).toBeVisible();
  });
});

test.describe("Forgot password page", () => {
  test("submits an email and shows the check-your-inbox confirmation", async ({ page }) => {
    await page.goto("/forgot-password");

    await page.getByLabel("Email address").fill("student@example.com");
    await page.getByRole("button", { name: "Send reset link" }).click();

    await expect(page.getByRole("heading", { name: "Check your inbox" })).toBeVisible();
    await expect(page.getByText("student@example.com")).toBeVisible();
    await expect(page.getByRole("link", { name: "Back to sign in" })).toBeVisible();
  });

  test("links back to the sign-in page", async ({ page }) => {
    await page.goto("/forgot-password");
    await page.getByRole("link", { name: "Sign in", exact: false }).click();
    await expect(page).toHaveURL(/\/login/);
  });
});
