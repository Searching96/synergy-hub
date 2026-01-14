import { test, expect } from "@playwright/test";

test.describe("Issue Lifecycle", () => {
    test.beforeEach(async ({ page }) => {
        // Standard login before each test
        await page.goto("/login");
        await page.getByPlaceholder(/Email/i).fill("test@example.com");
        await page.getByPlaceholder(/Password/i).fill("password123");
        await page.getByRole("button", { name: /Sign In/i }).click();
        await expect(page).toHaveURL(/.*board|.*dashboard/);
    });

    test("should create, move, and delete an issue", async ({ page }) => {
        const issueTitle = `E2E Task ${Date.now()}`;

        // 1. Create Issue
        await page.getByRole("button", { name: /Create Issue/i }).click();
        await page.getByLabel(/Summary/i).fill(issueTitle);
        await page.getByLabel(/Description/i).fill("Full lifecycle test");
        await page.getByRole("button", { name: /Create/i, exact: true }).click();

        // 2. Verify on board & Move status
        const card = page.getByText(issueTitle).first();
        await expect(card).toBeVisible();

        await card.click(); // Open detail modal
        await expect(page.getByRole("dialog")).toBeVisible();
        await expect(page.getByText(issueTitle)).toBeVisible();

        // Change status to IN_PROGRESS
        await page.getByLabel(/Status/i).click();
        await page.getByRole("option", { name: /In Progress/i }).click();

        // Wait for update (UI should show change)
        await expect(page.getByLabel(/Status/i)).toContainText(/In Progress/i);

        // 3. Delete Issue
        // Assuming there's a delete or actions menu in the modal
        // Let's check for an "Actions" or "Delete" button
        const deleteBtn = page.getByRole("button", { name: /Delete/i });
        if (await deleteBtn.isVisible()) {
            await deleteBtn.click();
        } else {
            // Try action menu
            await page.getByRole("button", { name: /More|Actions/i }).click();
            await page.getByRole("menuitem", { name: /Delete/i }).click();
        }

        // Confirm deletion
        await page.getByRole("button", { name: /Delete/i, exact: true }).click();

        // Verify it's gone from the board
        await expect(page.getByText(issueTitle)).not.toBeVisible();
    });
});

