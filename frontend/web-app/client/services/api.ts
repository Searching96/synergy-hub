import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import { toast } from "sonner";

const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 10000, // 10 second timeout
});

// Queue to store failed requests while token is being refreshed
interface QueuedRequest {
  config: InternalAxiosRequestConfig;
  resolve: (value: any) => void;
  reject: (error: any) => void;
}

let isRefreshing = false;
let failedQueue: QueuedRequest[] = [];

// Process queued requests after successful token refresh
const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.config.headers.Authorization = `Bearer ${token}`;
      prom.resolve(api(prom.config));
    }
  });

  failedQueue = [];
};

// Request Interceptor: Attach JWT token and organization ID
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Add X-Organization-ID header if organization ID is available
    // Check localStorage for organization context
    const orgIdFromLocalStorage = localStorage.getItem("organizationId");
    const userStr = localStorage.getItem("user");

    let organizationId: string | null = orgIdFromLocalStorage;

    if (!organizationId && userStr) {
      try {
        const user = JSON.parse(userStr);
        if (user.organizationId) {
          organizationId = user.organizationId.toString();
        }
      } catch (e) {
        console.debug("Failed to parse user for org ID");
      }
    }

    if (organizationId) {
      config.headers["X-Organization-ID"] = organizationId;
    }

    console.log(`[API Debug] Request to ${config.url}`, {
      headers: config.headers,
      method: config.method,
      tokenPresent: !!token,
      orgId: organizationId
    });

    return config;
  },
  (error) => Promise.reject(error)
);

// Valid task statuses - must match backend enum exactly
const VALID_TASK_STATUSES = ["TO_DO", "IN_PROGRESS", "IN_REVIEW", "DONE", "BLOCKED"] as const;

// Valid sprint statuses - must match backend enum exactly
const VALID_SPRINT_STATUSES = ["PLANNING", "ACTIVE", "COMPLETED", "CANCELLED"] as const;

// Helper function to validate and handle backend status values
const validateTaskStatus = (status: string, fieldName?: string): string => {
  // Accept valid task statuses as-is
  if (VALID_TASK_STATUSES.includes(status as any)) {
    return status;
  }

  // Accept valid sprint statuses as-is
  if (VALID_SPRINT_STATUSES.includes(status as any)) {
    return status;
  }

  // Accept valid meeting statuses as-is (fix for "Unknown status received: SCHEDULED")
  if (["SCHEDULED", "ENDED"].includes(status)) {
    return status;
  }

  // Unknown status - log for monitoring
  if (!VALID_TASK_STATUSES.includes(status as any) && !VALID_SPRINT_STATUSES.includes(status as any) && !["SCHEDULED", "ENDED"].includes(status)) {
    console.debug(`Unknown status received: "${status}"${fieldName ? ` in ${fieldName}` : ""}`);
  }

  return status;
};

const normalizeResponseData = (data: any): any => {
  if (!data) return data;

  // Normalize task status in single task response
  if (data.data && typeof data.data === "object" && data.data.status) {
    data.data.status = validateTaskStatus(data.data.status);
  }

  // Normalize task status in array responses
  if (data.data && Array.isArray(data.data)) {
    data.data = data.data.map((item: any) => {
      if (item && item.status) {
        return { ...item, status: validateTaskStatus(item.status) };
      }
      return item;
    });
  }

  // Normalize nested tasks in board/sprint responses
  if (data.data && data.data.activeSprints && Array.isArray(data.data.activeSprints)) {
    data.data.activeSprints = data.data.activeSprints.map((sprint: any) => {
      if (sprint.tasks && Array.isArray(sprint.tasks)) {
        sprint.tasks = sprint.tasks.map((task: any) => {
          if (task && task.status) {
            return { ...task, status: validateTaskStatus(task.status) };
          }
          return task;
        });
      }
      return sprint;
    });
  }

  return data;
};

// Response Interceptor: Handle 401 with token refresh, and 403 errors
api.interceptors.response.use(
  (response) => {
    // Normalize status enums in response data
    response.data = normalizeResponseData(response.data);
    return response;
  },
  (error) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const status = error.response?.status;
    const isAuthEndpoint =
      originalRequest.url?.includes("/auth/login") ||
      originalRequest.url?.includes("/auth/register") ||
      originalRequest.url?.includes("/auth/refresh");

    // Handle 401 Unauthorized - Token expired or invalid
    if (status === 401 && !isAuthEndpoint && !originalRequest._retry) {
      // If already refreshing, queue this request
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            config: originalRequest,
            resolve,
            reject,
          });
        });
      }

      // Mark that we're refreshing
      isRefreshing = true;
      originalRequest._retry = true;

      // Attempt to refresh token
      return (async () => {
        try {
          const refreshToken = localStorage.getItem("refreshToken");
          if (!refreshToken) {
            // No refresh token -> force logout and notify user
            const authError = new Error("No refresh token available");

            // Clear auth state
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            localStorage.removeItem("refreshToken");
            api.defaults.headers.common["Authorization"] = "";

            // Reject queued requests
            processQueue(authError, null);

            // Notify and redirect to login
            toast.error("Session expired. Please log in again.", { duration: 3000 });
            setTimeout(() => {
              window.location.href = "/login";
            }, 500);

            return Promise.reject(authError);
          }

          // Use raw axios instance (not api) to avoid interceptor recursion
          const response = await axios.post(
            `${import.meta.env.VITE_API_URL || "http://localhost:8080/api"}/auth/refresh`,
            { refreshToken },
            {
              timeout: 5000, // Shorter timeout for refresh to prevent hanging
            }
          );

          if (response.data.success && response.data.data.accessToken) {
            const newToken = response.data.data.accessToken;
            localStorage.setItem("token", newToken);

            if (response.data.data.refreshToken) {
              localStorage.setItem("refreshToken", response.data.data.refreshToken);
            }

            // Update authorization header for future requests
            api.defaults.headers.common["Authorization"] = `Bearer ${newToken}`;
            originalRequest.headers.Authorization = `Bearer ${newToken}`;

            // Process queued requests with new token
            processQueue(null, newToken);

            // Retry original request with new token
            return api(originalRequest);
          } else {
            throw new Error("Failed to refresh token");
          }
        } catch (refreshError) {
          // Refresh failed - clear auth and redirect to login
          localStorage.removeItem("token");
          localStorage.removeItem("user");
          localStorage.removeItem("refreshToken");

          api.defaults.headers.common["Authorization"] = "";

          // Process queued requests with error
          processQueue(refreshError, null);

          // Show user-friendly notification
          toast.error("Session expired. Please log in again.", {
            duration: 3000,
          });

          // Redirect to login
          setTimeout(() => {
            window.location.href = "/login";
          }, 500);

          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      })();
    }

    // Handle 403 Forbidden - User lacks permissions
    if (status === 403) {
      const errorMessage =
        error.response?.data?.message || "You don't have permission to perform this action.";
      // Show persistent error toast (5 seconds instead of 3) so user clearly sees permission issue
      toast.error(`Access Denied: ${errorMessage}`, {
        duration: 5000,
      });
      // CRITICAL FIX: Return rejection to stop mutation chain and prevent cache corruption
      return Promise.reject(error);
    }

    // Handle 5xx server errors
    if (status && status >= 500) {
      toast.error("Server error. Please try again later.", {
        duration: 3000,
      });
    }

    // Handle network timeout
    if (error.code === "ECONNABORTED") {
      toast.error("Request timeout. Please check your connection and try again.", {
        duration: 3000,
      });
    }

    return Promise.reject(error);
  }
);

export default api;
