import { test, expect } from "@playwright/test";

test.describe("Onboarding Flow", () => {
    test.beforeEach(async ({ page }) => {
        // Assume we have a user that needs onboarding
        // For E2E, we'll login with a test user
        await page.goto("/login");
        await page.getByPlaceholder(/Email/i).fill("test@example.com");
        await page.getByPlaceholder(/Password/i).fill("password123");
        await page.getByRole("button", { name: /Sign In/i }).click();

        // If they already have an org, they go to /dashboard or /board
        // If not, they go to /welcome
    });

    test("should allow a new user to create an organization", async ({ page }) => {
        // Ensure we are on the welcome page if the user has no org
        // (This assumes the test user is fresh or we handle the redirection logic)
        await page.goto("/welcome");

        await expect(page.getByText(/Welcome to Your Workspace/i)).toBeVisible();

        // Click on Create Organization card
        await page.getByRole("button", { name: /Create Organization/i, exact: false }).click();

        // Verify Dialog is open
        await expect(page.getByRole("dialog")).toBeVisible();
        await expect(page.getByText(/Create Your Organization/i)).toBeVisible();

        // Fill in details
        await page.getByLabel(/Organization Name/i).fill("E2E Test Org");
        await page.getByLabel(/Contact Email/i).fill("e2e-org@example.com");
        await page.getByLabel(/Address/i).fill("123 Playwright Way");

        // Submit
        await page.getByRole("button", { name: /Create Organization/i, exact: true }).click();

        // Verify success message
        await expect(page.getByText(/Organization created successfully/i)).toBeVisible();

        // Verify redirect to dashboard
        await expect(page).toHaveURL(/.*dashboard/, { timeout: 10000 });
    });

    test("should show error when creating organization without a name", async ({ page }) => {
        await page.goto("/welcome");
        await page.getByRole("button", { name: /Create Organization/i, exact: false }).click();

        // Try to submit empty name
        await page.getByRole("button", { name: /Create Organization/i, exact: true }).click();

        // Expect toast error (Sonner uses text content)
        await expect(page.getByText(/Organization name is required/i)).toBeVisible();
    });
});
