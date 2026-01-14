import { describe, it, expect, vi, afterAll, beforeAll } from "vitest";
import { formatDate, formatDateTime, formatRelativeTime, isPastDate, formatDateForInput } from "../date";

describe("Date Utils", () => {
    beforeAll(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date("2024-01-01T12:00:00Z"));
    });

    afterAll(() => {
        vi.useRealTimers();
    });

    describe("formatDate", () => {
        it("should format date string correctly", () => {
            // Adjust based on your locale settings if needed, but standard format usually works
            // Using a specific date to test formatting
            const date = "2024-01-01T00:00:00Z";
            // Check essential parts as locale might vary
            const formatted = formatDate(date);
            expect(formatted).toContain("Jan");
            expect(formatted).toContain("1");
            expect(formatted).toContain("2024");
        });

        it("should return empty string for null", () => {
            expect(formatDate(null as any)).toBe("");
        });
    });

    describe("formatRelativeTime", () => {
        it("should format 'Just now'", () => {
            // System time is 2024-01-01T12:00:00Z
            const now = new Date("2024-01-01T12:00:00Z").toISOString();
            expect(formatRelativeTime(now)).toBe("Just now");
        });

        it("should format '5 minutes ago'", () => {
            const fiveMinsAgo = new Date("2024-01-01T11:55:00Z").toISOString();
            expect(formatRelativeTime(fiveMinsAgo)).toBe("5 minutes ago");
        });

        it("should format '2 days ago'", () => {
            const twoDaysAgo = new Date("2023-12-30T12:00:00Z").toISOString();
            expect(formatRelativeTime(twoDaysAgo)).toBe("2 days ago");
        });
    });

    describe("isPastDate", () => {
        it("should return true for past dates", () => {
            const past = "2023-01-01T00:00:00Z";
            expect(isPastDate(past)).toBe(true);
        });

        it("should return false for future dates", () => {
            const future = "2024-02-01T00:00:00Z";
            expect(isPastDate(future)).toBe(false);
        });
    });

    describe("formatDateForInput", () => {
        it("should format date for HTML input", () => {
            const date = new Date("2024-01-15T10:30:00Z");
            expect(formatDateForInput(date)).toBe("2024-01-15");
        });
    });
});
