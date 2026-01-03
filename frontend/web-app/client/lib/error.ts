/**
 * Error handling utilities
 * Standardizes error message extraction from API responses
 */

import { AxiosError } from "axios";

/**
 * Extract error message from various error formats
 * Handles both Axios errors and generic errors
 * @param error - Error object from API call
 * @param fallback - Fallback message if no error message found
 * @returns Standardized error message string
 */
export function extractErrorMessage(
  error: unknown,
  fallback: string = "An unexpected error occurred"
): string {
  // Check if it's an Axios error
  if (error && typeof error === "object" && "response" in error) {
    const axiosError = error as AxiosError<{
      message?: string;
      error?: string;
      errors?: string[];
    }>;

    const responseData = axiosError.response?.data;

    // Try different common error response formats
    if (responseData) {
      // Backend sends { message: "error text" }
      if (responseData.message) {
        return responseData.message;
      }
      // Backend sends { error: "error text" }
      if (responseData.error) {
        return responseData.error;
      }
      // Backend sends { errors: ["error1", "error2"] }
      if (responseData.errors && Array.isArray(responseData.errors)) {
        return responseData.errors.join(", ");
      }
    }
  }

  // Check if it's a standard Error object
  if (error instanceof Error) {
    return error.message;
  }

  // If error is a string
  if (typeof error === "string") {
    return error;
  }

  // Fallback
  return fallback;
}

/**
 * Check if error is a specific HTTP status code
 * @param error - Error object
 * @param statusCode - HTTP status code to check
 * @returns true if error has the specified status code
 */
export function isErrorStatus(error: unknown, statusCode: number): boolean {
  if (error && typeof error === "object" && "response" in error) {
    const axiosError = error as AxiosError;
    return axiosError.response?.status === statusCode;
  }
  return false;
}

/**
 * Check if error is a 403 Forbidden error
 * @param error - Error object
 * @returns true if error is 403
 */
export function isForbiddenError(error: unknown): boolean {
  return isErrorStatus(error, 403);
}

/**
 * Check if error is a 401 Unauthorized error
 * @param error - Error object
 * @returns true if error is 401
 */
export function isUnauthorizedError(error: unknown): boolean {
  return isErrorStatus(error, 401);
}

/**
 * Check if error is a 404 Not Found error
 * @param error - Error object
 * @returns true if error is 404
 */
export function isNotFoundError(error: unknown): boolean {
  return isErrorStatus(error, 404);
}
