import { useState, useEffect } from "react";
import api from "@/services/api";
import type { AxiosError } from "axios";

interface PermissionError {
  message: string;
  timestamp: number;
}

/**
 * Hook to track and display persistent 403 permission errors
 * Unlike toast notifications that disappear, this maintains error state
 * until explicitly dismissed by the user
 */
export function usePermissionError() {
  const [error, setError] = useState<PermissionError | null>(null);

  useEffect(() => {
    // Add response interceptor to catch 403 errors
    const interceptorId = api.interceptors.response.use(
      (response) => response,
      (err: AxiosError<{ message?: string }>) => {
        if (err.response?.status === 403) {
          setError({
            message: err.response.data?.message || "You don't have permission to perform this action.",
            timestamp: Date.now(),
          });
        }
        // Don't swallow the error - let it propagate
        return Promise.reject(err);
      }
    );

    // Cleanup interceptor on unmount
    return () => {
      api.interceptors.response.eject(interceptorId);
    };
  }, []);

  const clearError = () => setError(null);

  return { 
    error: error?.message || null, 
    timestamp: error?.timestamp,
    clearError 
  };
}
