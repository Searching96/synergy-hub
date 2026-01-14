import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import EmailVerificationPage from '../EmailVerificationPage';
import authService from '@/services/auth.service';

// Mock dependencies
const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('@/services/auth.service', () => ({
    default: {
        verifyEmail: vi.fn(),
    },
}));

// Setup helper
const renderEmailVerificationPage = (token?: string) => {
    const initialEntry = token ? `/verify-email?token=${token}` : '/verify-email';
    return render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <EmailVerificationPage />
        </MemoryRouter>
    );
};

describe('EmailVerificationPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders loading state initially', () => {
        renderEmailVerificationPage('valid-token');

        expect(screen.getByText(/verifying email/i)).toBeInTheDocument();
        expect(screen.getByText(/please wait/i)).toBeInTheDocument();
    });

    it('shows error if no token is provided', () => {
        // No token set
        renderEmailVerificationPage();

        expect(screen.getByText(/verification failed/i)).toBeInTheDocument();
        expect(screen.getByText(/invalid verification link/i)).toBeInTheDocument();
    });

    it('calls verifyEmail and handles success', async () => {
        (authService.verifyEmail as any).mockResolvedValue({ success: true, message: 'Verified!' });

        renderEmailVerificationPage('valid-token');

        await waitFor(() => {
            expect(authService.verifyEmail).toHaveBeenCalledWith('', 'valid-token');
            expect(screen.getByText(/email verified!/i)).toBeInTheDocument();
            expect(screen.getByText(/redirecting to login/i)).toBeInTheDocument();
        });
    });

    it('handles verification failure', async () => {
        (authService.verifyEmail as any).mockResolvedValue({ success: false, message: 'Invalid token' });

        renderEmailVerificationPage('invalid-token');

        await waitFor(() => {
            expect(screen.getByText(/verification failed/i)).toBeInTheDocument();
            expect(screen.getByText(/invalid token/i)).toBeInTheDocument();
        });

        expect(screen.getByRole('button', { name: /go to login/i })).toBeInTheDocument();
    });

    it('handles API error', async () => {
        (authService.verifyEmail as any).mockRejectedValue({
            response: { data: { message: 'Network error' } }
        });

        renderEmailVerificationPage('error-token');

        await waitFor(() => {
            expect(screen.getByText(/verification failed/i)).toBeInTheDocument();
            expect(screen.getByText(/network error/i)).toBeInTheDocument();
        });
    });
});
