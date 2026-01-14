import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import RegisterPage from '../RegisterPage';
import { AuthProvider } from '@/context/AuthContext';

// Mock dependencies
const mockRegister = vi.fn();
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
            register: mockRegister,
            user: null,
            isAuthenticated: false,
            isLoading: false,
        }),
    };
});

// Setup helper
const renderRegisterPage = () => {
    return render(
        <MemoryRouter>
            <RegisterPage />
        </MemoryRouter>
    );
};

describe('RegisterPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders registration form correctly', () => {
        renderRegisterPage();

        expect(screen.getByText(/create an account/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/full name/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
        expect(screen.getAllByLabelText(/password/i).length).toBeGreaterThan(0);
        expect(screen.getByRole('button', { name: /sign up/i })).toBeInTheDocument();
    });

    it('validates passwords match', async () => {
        const user = userEvent.setup();
        renderRegisterPage();

        await user.type(screen.getByLabelText(/full name/i), 'Test User');
        await user.type(screen.getByLabelText(/email/i), 'test@example.com');
        await user.type(screen.getAllByLabelText(/password/i)[0], 'Password123!');
        await user.type(screen.getByLabelText(/confirm password/i), 'Mismatch123!');

        await user.click(screen.getByRole('button', { name: /sign up/i }));

        await waitFor(() => {
            expect(screen.getByText(/passwords don't match/i)).toBeInTheDocument();
        });
    });

    it('calls register function on valid submission', async () => {
        const user = userEvent.setup();
        renderRegisterPage();

        mockRegister.mockResolvedValue({ success: true });

        await user.type(screen.getByLabelText(/full name/i), 'Test User');
        await user.type(screen.getByLabelText(/email/i), 'test@example.com');
        await user.type(screen.getAllByLabelText(/password/i)[0], 'Password123!');
        await user.type(screen.getByLabelText(/confirm password/i), 'Password123!');

        await user.click(screen.getByRole('button', { name: /sign up/i }));

        await waitFor(() => {
            expect(mockRegister).toHaveBeenCalledWith({
                name: 'Test User',
                email: 'test@example.com',
                password: 'Password123!',
                confirmPassword: 'Password123!',
            });
            expect(screen.getByText(/registration successful/i)).toBeInTheDocument();
        });
    });

    it('displays error on failed registration', async () => {
        const user = userEvent.setup();
        renderRegisterPage();

        mockRegister.mockResolvedValue({ success: false, message: 'Email already exists' });

        await user.type(screen.getByLabelText(/full name/i), 'Test User');
        await user.type(screen.getByLabelText(/email/i), 'test@example.com');
        await user.type(screen.getAllByLabelText(/password/i)[0], 'Password123!');
        await user.type(screen.getByLabelText(/confirm password/i), 'Password123!');

        await user.click(screen.getByRole('button', { name: /sign up/i }));

        await waitFor(() => {
            expect(screen.getByText(/email already exists/i)).toBeInTheDocument();
        });
    });

    it('navigates to login page', () => {
        renderRegisterPage();
        const loginLink = screen.getByRole('link', { name: /sign in/i });
        expect(loginLink).toHaveAttribute('href', '/login');
    });
});
