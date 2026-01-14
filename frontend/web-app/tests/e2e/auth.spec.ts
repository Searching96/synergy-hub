import { test, expect } from "@playwright/test";

test.describe("Authentication Flow", () => {
    test("should allow a user to log in and stay logged in", async ({ page }) => {
        // Go to login page
        await page.goto("/login");

        // Fill in credentials
        await page.getByPlaceholder(/Email/i).fill("test@example.com");
        await page.getByPlaceholder(/Password/i).fill("password123");

        // Submit
        await page.getByRole("button", { name: /Sign In/i }).click();

        // Verify redirect to dashboard/board
        // Note: Adjust URL based on actual app routes
        await expect(page).toHaveURL(/.*board|.*dashboard/);

        // Verify toast or profile name
        // await expect(page.getByText(/Success/i)).toBeVisible();
    });

    test("should show error on invalid credentials", async ({ page }) => {
        await page.goto("/login");
        await page.getByPlaceholder(/Email/i).fill("wrong@example.com");
        await page.getByPlaceholder(/Password/i).fill("wrongpass");
        await page.getByRole("button", { name: /Sign In/i }).click();

        // Expect error toast or message
        await expect(page.getByText(/Invalid credentials|Failed/i)).toBeVisible();
    });

    test("should allow a user to register", async ({ page }) => {
        await page.goto("/register");

        await page.getByPlaceholder(/John Doe/i).fill("E2E Test User");
        await page.getByPlaceholder(/john@example.com/i).fill(`e2e-${Date.now()}@example.com`);
        await page.getByLabel("Password", { exact: true }).fill("Password123!");
        await page.getByLabel(/Confirm Password/i).fill("Password123!");

        await page.getByRole("button", { name: /Sign Up/i }).click();

        // Verify success card
        await expect(page.getByText(/Registration Successful/i)).toBeVisible();

        // Wait for redirect to login or check if it happens
        await expect(page).toHaveURL(/.*login/, { timeout: 10000 });
    });
});

