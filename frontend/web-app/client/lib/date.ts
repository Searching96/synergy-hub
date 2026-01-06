/**
 * Date formatting utilities
 * Provides consistent date/time formatting across the application
 */

/**
 * Format a date string or Date object to localized date string
 * @param date - ISO date string or Date object
 * @param options - Intl.DateTimeFormatOptions
 * @returns Formatted date string
 */
export function formatDate(
  date: string | Date,
  options: Intl.DateTimeFormatOptions = {
    year: "numeric",
    month: "short",
    day: "numeric",
  }
): string {
  if (!date) return "";
  const dateObj = typeof date === "string" ? new Date(date) : date;
  return dateObj.toLocaleDateString(undefined, options);
}

/**
 * Format a date string or Date object to localized date and time string
 * @param date - ISO date string or Date object
 * @returns Formatted date and time string
 */
export function formatDateTime(date: string | Date): string {
  if (!date) return "";
  const dateObj = typeof date === "string" ? new Date(date) : date;
  return dateObj.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Format a date to relative time (e.g., "2 hours ago", "3 days ago")
 * @param date - ISO date string or Date object
 * @returns Relative time string
 */
export function formatRelativeTime(date: string | Date): string {
  if (!date) return "";
  const dateObj = typeof date === "string" ? new Date(date) : date;
  const now = new Date();
  const diffMs = now.getTime() - dateObj.getTime();
  const diffSecs = Math.floor(diffMs / 1000);
  const diffMins = Math.floor(diffSecs / 60);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);
  const diffWeeks = Math.floor(diffDays / 7);
  const diffMonths = Math.floor(diffDays / 30);
  const diffYears = Math.floor(diffDays / 365);

  if (diffSecs < 60) return "Just now";
  if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? "s" : ""} ago`;
  if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? "s" : ""} ago`;
  if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? "s" : ""} ago`;
  if (diffWeeks < 4) return `${diffWeeks} week${diffWeeks > 1 ? "s" : ""} ago`;
  if (diffMonths < 12) return `${diffMonths} month${diffMonths > 1 ? "s" : ""} ago`;
  return `${diffYears} year${diffYears > 1 ? "s" : ""} ago`;
}

/**
 * Check if a date is in the past
 * Uses UTC-aware parsing to prevent timezone issues
 * @param date - ISO date string or Date object
 * @returns true if the date is in the past
 * 
 * IMPORTANT: Backend MUST be configured to send dates in ISO 8601 format with explicit timezone:
 * - Spring Boot: Set spring.jackson.time-zone=UTC in application.yml
 * - Dates should include 'Z' suffix (e.g., "2026-01-15T14:30:00Z")
 * - Without 'Z', dates will be parsed in browser's local timezone causing incorrect calculations
 */
export function isPastDate(date: string | Date): boolean {
  if (!date) return false;
  
  let dateObj: Date;
  
  if (typeof date === "string") {
    // Parse ISO strings - browser handles timezone conversion automatically
    // if date includes timezone indicator (Z or offset)
    const hasTimezone = /Z|[+-]\d{2}:\d{2}$/.test(date);
    
    if (!hasTimezone) {
      // WARNING: Date without timezone will be parsed as local time
      // This may cause incorrect due date calculations if backend timezone != user timezone
      console.warn(
        `Date "${date}" missing timezone. Configure backend to send ISO 8601 with 'Z' suffix. ` +
        `See: spring.jackson.time-zone=UTC`
      );
    }
    
    dateObj = new Date(date);
  } else {
    dateObj = date;
  }
  
  return dateObj.getTime() < Date.now();
}

/**
 * Format date for input[type="date"] value (YYYY-MM-DD)
 * @param date - ISO date string or Date object
 * @returns Date string in YYYY-MM-DD format
 */
export function formatDateForInput(date: string | Date): string {
  if (!date) return "";
  const dateObj = typeof date === "string" ? new Date(date) : date;
  return dateObj.toISOString().split("T")[0];
}
