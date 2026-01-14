import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import LoginPage from '../LoginPage';
import { AuthProvider } from '@/context/AuthContext';
import authService from '@/services/auth.service';

// Mock dependencies
const mockLogin = vi.fn();
const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('@/context/AuthContext', async () => {
    const actual = await vi.importActual('@/context/AuthContext');
    return {
        ...actual,
        useAuth: () => ({
            login: mockLogin,
            user: null,
            isAuthenticated: false,
            isLoading: false,
        }),
    };
});

vi.mock('@/services/auth.service', () => ({
    default: {
        resendVerificationEmail: vi.fn(),
    },
}));

// Setup helper
const renderLoginPage = () => {
    return render(
        <MemoryRouter>
            <LoginPage />
        </MemoryRouter>
    );
};

describe('LoginPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders login form correctly', () => {
        renderLoginPage();

        expect(screen.getByText(/welcome to synergyhub/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    });

    it('validates required fields', async () => {
        const user = userEvent.setup();
        renderLoginPage();

        const submitButton = screen.getByRole('button', { name: /sign in/i });
        await user.click(submitButton);

        await waitFor(() => {
            expect(screen.getByText(/invalid email address/i)).toBeInTheDocument();
            expect(screen.getByText(/password is required/i)).toBeInTheDocument();
        });
    });

    it('calls login function on valid submission', async () => {
        const user = userEvent.setup();
        renderLoginPage();

        mockLogin.mockResolvedValue({ success: true, data: { requiresTwoFactor: false } });

        await user.type(screen.getByLabelText(/email/i), 'test@example.com');
        await user.type(screen.getByLabelText(/password/i), 'password123');

        await user.click(screen.getByRole('button', { name: /sign in/i }));

        await waitFor(() => {
            expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'password123', undefined);
            expect(mockNavigate).toHaveBeenCalledWith('/projects');
        });
    });

    it('displays error on failed login', async () => {
        const user = userEvent.setup();
        renderLoginPage();

        mockLogin.mockResolvedValue({ success: false, message: 'Invalid credentials' });

        await user.type(screen.getByLabelText(/email/i), 'test@example.com');
        await user.type(screen.getByLabelText(/password/i), 'password123');

        await user.click(screen.getByRole('button', { name: /sign in/i }));

        await waitFor(() => {
            expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument();
        });
    });

    it('handles 2FA requirement', async () => {
        const user = userEvent.setup();
        renderLoginPage();

        // First attempt triggers 2FA requirement
        mockLogin.mockResolvedValueOnce({ success: true, data: { requiresTwoFactor: true } });

        await user.type(screen.getByLabelText(/email/i), 'test@example.com');
        await user.type(screen.getByLabelText(/password/i), 'password123');
        await user.click(screen.getByRole('button', { name: /sign in/i }));

        await waitFor(() => {
            expect(screen.getByLabelText(/2fa code/i)).toBeInTheDocument();
        });

        // Second attempt with 2FA code
        mockLogin.mockResolvedValueOnce({ success: true, data: { requiresTwoFactor: false } });

        await user.type(screen.getByLabelText(/2fa code/i), '123456');
        await user.click(screen.getByRole('button', { name: /sign in/i }));

        await waitFor(() => {
            expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'password123', '123456');
        });
    });

    it('navigates to register page', () => {
        renderLoginPage();
        const registerLink = screen.getByRole('link', { name: /sign up/i });
        expect(registerLink).toHaveAttribute('href', '/register');
    });

    it('navigates to forgot password page', () => {
        renderLoginPage();
        const forgotPasswordLink = screen.getByRole('link', { name: /forgot password/i });
        expect(forgotPasswordLink).toHaveAttribute('href', '/forgot-password');
    });
});
