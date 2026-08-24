import { test, expect } from "@playwright/test";

test.describe("Login page", () => {
  test("renders the sign-in form with both fields", async ({ page }) => {
    await page.goto("/login");

    await expect(page.getByRole("heading", { name: "Sign in to your account" })).toBeVisible();
    await expect(page.getByLabel("Email or Student ID")).toBeVisible();
    await expect(page.getByLabel("Password")).toBeVisible();
    await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();
  });

  test("shows validation errors when submitting an empty form", async ({ page }) => {
    await page.goto("/login");

    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page.getByText("Enter your email or Student ID")).toBeVisible();
    await expect(page.getByText("Password is required")).toBeVisible();
  });

  test("toggles password visibility", async ({ page }) => {
    await page.goto("/login");

    const passwordInput = page.getByLabel("Password");
    await passwordInput.fill("some-secret");
    await expect(passwordInput).toHaveAttribute("type", "password");

    await page.getByRole("button", { name: "Show password" }).click();
    await expect(passwordInput).toHaveAttribute("type", "text");

    await page.getByRole("button", { name: "Hide password" }).click();
    await expect(passwordInput).toHaveAttribute("type", "password");
  });

  test("redirects an unauthenticated visitor away from an admin route", async ({ page }) => {
    await page.goto("/admin/settings");
    await expect(page).toHaveURL(/\/login/);
  });
});
