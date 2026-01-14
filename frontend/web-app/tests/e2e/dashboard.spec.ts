import { test, expect } from "@playwright/test";

test.describe("Dashboard (Your Work)", () => {
    test.beforeEach(async ({ page }) => {
        // Login
        await page.goto("/login");
        await page.getByPlaceholder(/Email/i).fill("test@example.com");
        await page.getByPlaceholder(/Password/i).fill("password123");
        await page.getByRole("button", { name: /Sign In/i }).click();

        // Redirection to dashboard is typical after login
        await expect(page).toHaveURL(/.*dashboard/);
    });

    test("should display Your Work header and tabs", async ({ page }) => {
        await expect(page.getByText(/Your Work/i, { exact: true })).toBeVisible();
        await expect(page.getByRole("tab", { name: /Assigned to Me/i })).toBeVisible();
        await expect(page.getByRole("tab", { name: /Recent Projects/i })).toBeVisible();
    });

    test("should show assigned issues or empty state", async ({ page }) => {
        await page.getByRole("tab", { name: /Assigned to Me/i }).click();

        // Either a table or an empty message
        const hasIssues = await page.locator("table").isVisible();
        if (!hasIssues) {
            await expect(page.getByText(/No assigned issues yet/i)).toBeVisible();
        } else {
            await expect(page.locator("table")).toBeVisible();
        }
    });

    test("should show recent projects or empty state", async ({ page }) => {
        await page.getByRole("tab", { name: /Recent Projects/i }).click();

        const hasProjectCards = await page.locator(".grid .border").first().isVisible();
        if (!hasProjectCards) {
            await expect(page.getByText(/No recent projects found/i)).toBeVisible();
            await expect(page.getByRole("link", { name: /View All Projects/i })).toBeVisible();
        } else {
            await expect(page.locator(".grid .border").first()).toBeVisible();
        }
    });
});
