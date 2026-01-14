import { test, expect } from "@playwright/test";

test.describe("Advanced Project Views", () => {
    let projectId: string;

    test.beforeEach(async ({ page }) => {
        // Login
        await page.goto("/login");
        await page.getByPlaceholder(/Email/i).fill("test@example.com");
        await page.getByPlaceholder(/Password/i).fill("password123");
        await page.getByRole("button", { name: /Sign In/i }).click();

        // Navigate to a project
        await page.goto("/projects");
        const firstProject = page.locator(".grid .cursor-pointer").first();
        await expect(firstProject).toBeVisible();
        await firstProject.click();
        await expect(page).toHaveURL(/.*board/);

        projectId = page.url().split("/")[4];
    });

    test("should display List view with tasks", async ({ page }) => {
        await page.goto(`/projects/${projectId}/list`);
        await expect(page.getByText(/List/i, { exact: true })).toBeVisible();

        // Check for table headers
        await expect(page.getByRole("columnheader", { name: /Summary/i })).toBeVisible();

        // Check for search functionality
        const searchInput = page.getByPlaceholder(/Search issues/i);
        await searchInput.fill("Test Task");
        // Verify results (might be empty but loader/table should be there)
        await expect(page.locator("table")).toBeVisible();
    });

    test("should display Timeline view", async ({ page }) => {
        await page.goto(`/projects/${projectId}/timeline`);
        await expect(page.getByText(/Timeline/i, { exact: true })).toBeVisible();
        await expect(page.getByText(/Epics & Tasks/i)).toBeVisible();

        // Check for months on time axis
        const axisText = await page.locator(".grid div").first().textContent();
        expect(axisText).not.toBeNull();
    });

    test("should display Activity view", async ({ page }) => {
        await page.goto(`/projects/${projectId}/activity`);
        await expect(page.getByText(/Activity Stream/i)).toBeVisible();

        // ActivityStream should render something, even if just "No activity" message inside it
        await expect(page.locator(".space-y-6")).toBeVisible();
    });
});
