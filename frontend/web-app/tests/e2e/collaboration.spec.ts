import { test, expect } from "@playwright/test";

test.describe("Collaboration Flow", () => {
    test.beforeEach(async ({ page }) => {
        // Login
        await page.goto("/login");
        await page.getByPlaceholder(/Email/i).fill("test@example.com");
        await page.getByPlaceholder(/Password/i).fill("password123");
        await page.getByRole("button", { name: /Sign In/i }).click();

        // Navigate to the first project
        await page.goto("/projects");
        const firstProject = page.locator(".grid .cursor-pointer").first();
        await expect(firstProject).toBeVisible();
        await firstProject.click();
        await expect(page).toHaveURL(/.*board/);
    });

    test("should send a chat message", async ({ page }) => {
        // Navigate to Chat
        // Assuming side navigation or direct URL
        const projectId = page.url().split("/")[4];
        await page.goto(`/projects/${projectId}/chat`);

        await expect(page.getByText(/Project Chat/i)).toBeVisible();

        const messageText = `Hello Team! Test ${Date.now()}`;

        // Find input and send
        await page.getByPlaceholder(/Type a message/i).fill(messageText);
        await page.keyboard.press("Enter");

        // Verify message appears (optimistically or after sync)
        await expect(page.getByText(messageText)).toBeVisible({ timeout: 10000 });
    });

    test("should create a new meeting", async ({ page }) => {
        // Navigate to Meetings
        const projectId = page.url().split("/")[4];
        await page.goto(`/projects/${projectId}/meetings`);

        await expect(page.getByText(/Meetings/i, { exact: true })).toBeVisible();

        // Use "New Meeting" button
        await page.getByRole("button", { name: /New Meeting/i }).click();

        const meetingTitle = `Sprint Sync ${Date.now()}`;
        await page.getByLabel(/Meeting Title/i).fill(meetingTitle);
        await page.getByLabel(/Description/i).fill("Discussing E2E progress");

        await page.getByRole("button", { name: /Start Meeting/i }).click();

        // Verify redirection to meeting room
        await expect(page).toHaveURL(/.*meetings\/.*/);
        await expect(page.getByText(meetingTitle)).toBeVisible();

        // Go back to list and check if it exists
        await page.goto(`/projects/${projectId}/meetings`);
        await expect(page.getByText(meetingTitle)).toBeVisible();
    });
});
