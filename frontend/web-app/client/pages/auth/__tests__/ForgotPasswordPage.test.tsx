import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ForgotPasswordPage from '../ForgotPasswordPage';
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
        forgotPassword: vi.fn(),
    },
}));

// Setup helper
const renderForgotPasswordPage = () => {
    return render(
        <MemoryRouter>
            <ForgotPasswordPage />
        </MemoryRouter>
    );
};

describe('ForgotPasswordPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders forgot password form correctly', () => {
        renderForgotPasswordPage();

        expect(screen.getByText(/forgot password/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /send reset link/i })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /back to login/i })).toBeInTheDocument();
    });

    it.skip('validates email format', async () => {
        renderForgotPasswordPage();

        const emailInput = screen.getByLabelText(/email address/i);
        // Use fireEvent to bypass potential userEvent issues with email inputs
        const { fireEvent } = await import('@testing-library/react');

        fireEvent.change(emailInput, { target: { value: 'invalid-email' } });

        const submitButton = screen.getByRole('button', { name: /send reset link/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(screen.getByText(/invalid email address/i)).toBeInTheDocument();
        });
    });

    it('calls forgotPassword API on valid submission', async () => {
        const user = userEvent.setup();
        renderForgotPasswordPage();

        (authService.forgotPassword as any).mockResolvedValue({ success: true });

        await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
        await user.click(screen.getByRole('button', { name: /send reset link/i }));

        await waitFor(() => {
            expect(authService.forgotPassword).toHaveBeenCalledWith('test@example.com');
            expect(screen.getByText(/check your email/i)).toBeInTheDocument();
        });
    });

    it('displays error on API failure', async () => {
        const user = userEvent.setup();
        renderForgotPasswordPage();

        (authService.forgotPassword as any).mockRejectedValue({
            response: { data: { message: 'Failed to process request' } }
        });

        await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
        await user.click(screen.getByRole('button', { name: /send reset link/i }));

        await waitFor(() => {
            expect(screen.getByText(/failed to process request/i)).toBeInTheDocument();
        });
    });

    it('allows navigation back to login', () => {
        renderForgotPasswordPage();
        const loginLink = screen.getByRole('link', { name: /back to login/i });
        expect(loginLink).toHaveAttribute('href', '/login');
    });
});
