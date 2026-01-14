import { test, expect } from "@playwright/test";

test.describe("Teams Management", () => {
    test.beforeEach(async ({ page }) => {
        // Login
        await page.goto("/login");
        await page.getByPlaceholder(/Email/i).fill("test@example.com");
        await page.getByPlaceholder(/Password/i).fill("password123");
        await page.getByRole("button", { name: /Sign In/i }).click();
        await expect(page).toHaveURL(/.*dashboard/);
    });

    test("should create a new team", async ({ page }) => {
        await page.goto("/teams/create");
        await expect(page.getByText(/Create a Team/i)).toBeVisible();

        const teamName = `Alpha Team ${Date.now()}`;
        await page.getByLabel(/Team Name/i).fill(teamName);
        await page.getByPlaceholder(/What is this team responsible for?/i).fill("Managing core platform stability");

        await page.getByRole("button", { name: /Save Team/i }).click();

        // Success toast check (sonner)
        await expect(page.getByText(/Team created successfully!/i)).toBeVisible();

        // Redirects back (usually to dashboard or wherever user came from)
        // Since navigate(-1) is used, we just check we are not on the create page anymore
        await expect(page).not.toHaveURL(/.*teams\/create/);
    });

    test("should validate required team name", async ({ page }) => {
        await page.goto("/teams/create");

        // Submit without name
        await page.getByRole("button", { name: /Save Team/i }).click();

        // HTML5 validation or application level check
        // If it's HTML5 'required', the URL shouldn't change
        await expect(page).toHaveURL(/.*teams\/create/);
    });
});
