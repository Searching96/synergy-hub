import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import Home from '../Home';

const renderHome = () => {
    return render(
        <MemoryRouter>
            <Home />
        </MemoryRouter>
    );
};

describe('LandingHome', () => {
    it('renders hero section correctly', () => {
        renderHome();

        // Use accessible name check for the heading which encompasses all child text nodes and ignores split lines
        expect(screen.getByRole('heading', { level: 1, name: /collaborate\. innovate\. ship\./i })).toBeInTheDocument();

        // Check for paragraph text using partial match
        expect(screen.getByText(/all-in-one platform/i)).toBeInTheDocument();
    });

    it('renders navigation links', () => {
        renderHome();

        const signInLinks = screen.getAllByRole('link', { name: /sign in/i });
        expect(signInLinks.length).toBeGreaterThan(0);
        expect(signInLinks[0]).toHaveAttribute('href', '/login');

        const getStartedLinks = screen.getAllByRole('link', { name: /get started/i });
        expect(getStartedLinks.length).toBeGreaterThan(0);
        expect(getStartedLinks[0]).toHaveAttribute('href', '/register');

        const startFreeLink = screen.getByRole('link', { name: /start for free/i });
        expect(startFreeLink).toHaveAttribute('href', '/register');
    });

    it('renders feature grid', () => {
        renderHome();

        expect(screen.getByText(/project management/i)).toBeInTheDocument();
        expect(screen.getByText(/real-time chat/i)).toBeInTheDocument();
        expect(screen.getByText(/video meetings/i)).toBeInTheDocument();
    });

    it('renders footer', () => {
        renderHome();
        expect(screen.getByText(/synergyhub. all rights reserved/i)).toBeInTheDocument();
    });
});
