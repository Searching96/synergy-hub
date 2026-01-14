import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import DashboardHome from '../Home';
import { useProjects } from '@/hooks/useProjects';

// Mock dependencies
const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('@/hooks/useProjects', () => ({
    useProjects: vi.fn(),
}));

const renderDashboardHome = () => {
    return render(
        <MemoryRouter>
            <DashboardHome />
        </MemoryRouter>
    );
};

const mockProjectsData = {
    data: {
        content: [
            {
                id: '1',
                name: 'Active Project',
                status: 'ACTIVE',
                role: 'OWNER',
                memberCount: 2,
                taskCount: 5,
                completedTaskCount: 2,
            },
            {
                id: '2',
                name: 'Archived Project',
                status: 'ARCHIVED',
                role: 'MEMBER',
                memberCount: 1,
                taskCount: 0,
                completedTaskCount: 0,
            }
        ]
    }
};

describe('DashboardHome', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders loading state', () => {
        (useProjects as any).mockReturnValue({
            isLoading: true,
            isError: false,
        });

        const { container } = renderDashboardHome();
        expect(container.getElementsByClassName('animate-pulse').length).toBeGreaterThan(0);
    });

    it('renders error state', () => {
        (useProjects as any).mockReturnValue({
            isLoading: false,
            isError: true,
        });

        renderDashboardHome();
        expect(screen.getByText(/failed to load projects/i)).toBeInTheDocument();
    });

    it('renders active projects only', () => {
        (useProjects as any).mockReturnValue({
            isLoading: false,
            isError: false,
            data: mockProjectsData,
        });

        renderDashboardHome();

        expect(screen.getByText('Active Project')).toBeInTheDocument();
        expect(screen.queryByText('Archived Project')).not.toBeInTheDocument();
        expect(screen.getByText('OWNER')).toBeInTheDocument();
        expect(screen.getByText('40%')).toBeInTheDocument(); // 2/5 * 100
    });

    it('handles empty state', () => {
        (useProjects as any).mockReturnValue({
            isLoading: false,
            isError: false,
            data: { data: { content: [] } },
        });

        renderDashboardHome();
        expect(screen.getByText(/no projects yet/i)).toBeInTheDocument();
        expect(screen.getByText(/create your first project/i)).toBeInTheDocument();
    });

    it('navigates to project board on click', async () => {
        const user = userEvent.setup();
        (useProjects as any).mockReturnValue({
            isLoading: false,
            isError: false,
            data: mockProjectsData,
        });

        renderDashboardHome();

        await user.click(screen.getByText('Active Project'));

        expect(mockNavigate).toHaveBeenCalledWith('/projects/1/board');
    });
});
