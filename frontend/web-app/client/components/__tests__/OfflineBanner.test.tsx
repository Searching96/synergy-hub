import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { OfflineBanner } from '../OfflineBanner';

// Mock framer-motion to avoid animation delays
vi.mock('framer-motion', () => ({
    AnimatePresence: ({ children }: any) => <>{children}</>,
    motion: {
        div: ({ children, className }: any) => <div className={className}>{children}</div>,
    }
}));

describe('OfflineBanner', () => {

    const originalNavigator = window.navigator;

    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
        Object.defineProperty(window, 'navigator', {
            value: originalNavigator,
            writable: true
        });
    });

    it('renders nothing when online initially', () => {
        Object.defineProperty(window.navigator, 'onLine', { value: true, configurable: true });
        render(<OfflineBanner />);
        expect(screen.queryByText(/you are offline/i)).not.toBeInTheDocument();
    });

    it('shows offline message when offline initially', () => {
        Object.defineProperty(window.navigator, 'onLine', { value: false, configurable: true });
        render(<OfflineBanner />);
        expect(screen.getByText(/you are offline/i)).toBeInTheDocument();
    });

    it('shows offline message when window goes offline', () => {
        Object.defineProperty(window.navigator, 'onLine', { value: true, configurable: true });
        render(<OfflineBanner />);

        act(() => {
            window.dispatchEvent(new Event('offline'));
        });

        expect(screen.getByText(/you are offline/i)).toBeInTheDocument();
    });

    it('shows reconnected message then hides it when coming back online', () => {
        Object.defineProperty(window.navigator, 'onLine', { value: false, configurable: true });
        render(<OfflineBanner />);

        // Go online
        act(() => {
            window.dispatchEvent(new Event('online'));
        });

        expect(screen.getByText(/connected! syncing data/i)).toBeInTheDocument();
        expect(screen.queryByText(/you are offline/i)).not.toBeInTheDocument();

        // Wait for timeout (3000ms)
        act(() => {
            vi.advanceTimersByTime(3000);
        });

        expect(screen.queryByText(/connected! syncing data/i)).not.toBeInTheDocument();
    });
});
