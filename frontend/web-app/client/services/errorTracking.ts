/**
 * Error Tracking Service
 * 
 * Provides an abstraction layer for reporting errors in production.
 * Currently logs to console with structured metadata, but can be 
 * easily connected to Sentry, LogRocket, or Datadog.
 */

export interface ErrorContext {
    componentName?: string;
    userId?: string | number;
    organizationId?: string | number;
    action?: string;
    url?: string;
    [key: string]: any;
}

class ErrorTrackingService {
    private static instance: ErrorTrackingService;
    private isDevelopment = import.meta.env.DEV;

    private constructor() { }

    public static getInstance(): ErrorTrackingService {
        if (!ErrorTrackingService.instance) {
            ErrorTrackingService.instance = new ErrorTrackingService();
        }
        return ErrorTrackingService.instance;
    }

    /**
     * Captures an exception and reports it with context.
     */
    public captureException(error: Error | unknown, context: ErrorContext = {}): void {
        const errorObj = error instanceof Error ? error : new Error(String(error));

        // Add global context from localStorage if available
        const enrichedContext = {
            ...context,
            timestamp: new Date().toISOString(),
            url: window.location.href,
            userAgent: navigator.userAgent,
            organizationId: localStorage.getItem("organizationId") || undefined,
        };

        if (this.isDevelopment) {
            console.group(`🔴 Error Captured: ${errorObj.message}`);
            console.error(errorObj);
            console.table(enrichedContext);
            console.groupEnd();
        } else {
            // PROD: Implement real tracking here (e.g., Sentry.captureException)
            // For now, we simulate by sending a structured log
            console.error("[PROD ERROR]", {
                message: errorObj.message,
                stack: errorObj.stack,
                context: enrichedContext,
            });
        }
    }

    /**
     * Logs a business-level event or warning.
     */
    public captureMessage(message: string, level: "info" | "warning" | "error" = "info", context: ErrorContext = {}): void {
        if (this.isDevelopment) {
            console.log(`[${level.toUpperCase()}] ${message}`, context);
        } else {
            // Send to monitoring service
            console.warn("[PROD MSG]", { message, level, context });
        }
    }
}

export const errorTracking = ErrorTrackingService.getInstance();
