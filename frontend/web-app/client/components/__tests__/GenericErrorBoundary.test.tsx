import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, afterEach } from 'vitest';
import { GenericErrorBoundary } from '../GenericErrorBoundary';
import { errorTracking } from '@/services/errorTracking';

// Mock dependencies
vi.mock('@/services/errorTracking', () => ({
    errorTracking: {
        captureException: vi.fn(),
    },
}));

// Test component that throws error
const Bomb = ({ shouldThrow }: { shouldThrow?: boolean }) => {
    if (shouldThrow) {
        throw new Error('Boom!');
    }
    return <div>Safe Content</div>;
};

// Silence console.error for expected errors
const originalConsoleError = console.error;
const consoleSpy = vi.spyOn(console, 'error');

describe('GenericErrorBoundary', () => {
    afterEach(() => {
        vi.clearAllMocks();
        consoleSpy.mockClear();
    });

    it('renders children normally when no error', () => {
        render(
            <GenericErrorBoundary>
                <div>Healthy Content</div>
            </GenericErrorBoundary>
        );
        expect(screen.getByText('Healthy Content')).toBeInTheDocument();
        expect(screen.queryByText(/something went wrong/i)).not.toBeInTheDocument();
    });

    it('catches errors and renders fallback UI', () => {
        // Suppress expected console.error from React boundary
        consoleSpy.mockImplementation(() => { });

        render(
            <GenericErrorBoundary>
                <Bomb shouldThrow={true} />
            </GenericErrorBoundary>
        );

        expect(screen.getByText(/something went wrong/i)).toBeInTheDocument();
        expect(screen.getByText(/unexpected error/i)).toBeInTheDocument();
        expect(errorTracking.captureException).toHaveBeenCalled();
    });

    it('renders custom fallback if provided', () => {
        consoleSpy.mockImplementation(() => { });

        render(
            <GenericErrorBoundary fallback={<div>Custom Error Message</div>}>
                <Bomb shouldThrow={true} />
            </GenericErrorBoundary>
        );

        expect(screen.getByText('Custom Error Message')).toBeInTheDocument();
        expect(screen.queryByText(/something went wrong/i)).not.toBeInTheDocument();
    });

    it('resets error state on "Try Again" click', () => {
        consoleSpy.mockImplementation(() => { });

        const { rerender } = render(
            <GenericErrorBoundary>
                <Bomb shouldThrow={true} />
            </GenericErrorBoundary>
        );

        expect(screen.getByText(/something went wrong/i)).toBeInTheDocument();

        // Rerender with safe content but boundary is still in error state
        rerender(
            <GenericErrorBoundary>
                <div>Safe Now</div>
            </GenericErrorBoundary>
        );

        // Click Try Again
        const tryAgainButton = screen.getByRole('button', { name: /try again/i });
        fireEvent.click(tryAgainButton);

        // Should now show safe content
        expect(screen.getByText('Safe Now')).toBeInTheDocument();
    });
});
