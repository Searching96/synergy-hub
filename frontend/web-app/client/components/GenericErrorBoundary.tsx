import React, { Component, ErrorInfo, ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { AlertTriangle, RefreshCcw } from "lucide-react";
import { errorTracking } from "@/services/errorTracking";

interface Props {
    children?: ReactNode;
    fallback?: ReactNode;
    name?: string;
}

interface State {
    hasError: boolean;
    error?: Error;
}

export class GenericErrorBoundary extends Component<Props, State> {
    public state: State = {
        hasError: false,
    };

    public static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        errorTracking.captureException(error, {
            componentName: this.props.name || "Unknown Component",
            errorInfo: errorInfo.componentStack,
        });
    }

    private handleReset = () => {
        this.setState({ hasError: false, error: undefined });
    };

    public render() {
        if (this.state.hasError) {
            if (this.props.fallback) {
                return this.props.fallback;
            }

            return (
                <div className="flex flex-col items-center justify-center min-h-[200px] p-6 text-center bg-red-50/50 rounded-lg border border-red-100">
                    <AlertTriangle className="h-10 w-10 text-red-500 mb-4" />
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">Something went wrong</h3>
                    <p className="text-sm text-gray-600 mb-6 max-w-md mx-auto">
                        The application encountered an unexpected error in {this.props.name || "this section"}.
                    </p>
                    <div className="flex gap-3">
                        <Button variant="outline" onClick={() => window.location.reload()}>
                            <RefreshCcw className="h-4 w-4 mr-2" />
                            Reload Page
                        </Button>
                        <Button onClick={this.handleReset}>
                            Try Again
                        </Button>
                    </div>
                    {import.meta.env.DEV && this.state.error && (
                        <pre className="mt-8 p-4 bg-gray-900 text-red-400 text-left overflow-auto max-w-full text-xs rounded">
                            {this.state.error.stack}
                        </pre>
                    )}
                </div>
            );
        }

        return this.props.children;
    }
}
