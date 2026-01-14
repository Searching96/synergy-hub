import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ProjectsPage from '../ProjectsPage';
import { useProjects, useCreateProject } from '@/hooks/useProjects';

// Mock dependencies
const mockNavigate = vi.fn();
const mockToast = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('@/hooks/use-toast', () => ({
    useToast: () => ({ toast: mockToast }),
}));

vi.mock('@/hooks/useProjects', () => ({
    useProjects: vi.fn(),
    useCreateProject: vi.fn(),
}));

// Setup helper
const renderProjectsPage = () => {
    return render(
        <MemoryRouter>
            <ProjectsPage />
        </MemoryRouter>
    );
};

// Mock data
const mockProjectsData = {
    data: {
        content: [
            {
                id: '1',
                name: 'Project Alpha',
                description: 'First project',
                status: 'ACTIVE',
                memberCount: 3,
                taskCount: 10,
                completedTaskCount: 5,
                startDate: '2023-01-01',
                endDate: '2023-12-31',
            },
            {
                id: '2',
                name: 'Project Beta',
                description: 'Second project',
                status: 'ACTIVE',
                memberCount: 5,
                taskCount: 20,
                completedTaskCount: 20, // 100%
            },
        ],
        totalPages: 2,
        totalElements: 12,
    },
};

const mockCreateProjectMutate = vi.fn();

describe('ProjectsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        (useCreateProject as any).mockReturnValue({
            mutateAsync: mockCreateProjectMutate,
            isPending: false,
        });
    });

    it('renders loading state', () => {
        (useProjects as any).mockReturnValue({
            isLoading: true,
            isError: false,
            data: undefined,
        });

        const { container } = renderProjectsPage();
        // Check for skeletons or loading structure. 
        // Logic relies on seeing "skeleton" classes or structure since there's no text "Loading..."
        expect(container.getElementsByClassName('animate-pulse').length).toBeGreaterThan(0);
    });

    it('renders error state', () => {
        (useProjects as any).mockReturnValue({
            isLoading: false,
            isError: true,
            data: undefined,
        });

        renderProjectsPage();
        expect(screen.getByText(/failed to load projects/i)).toBeInTheDocument();
    });

    it('renders project list', () => {
        (useProjects as any).mockReturnValue({
            isLoading: false,
            isError: false,
            data: mockProjectsData,
        });

        renderProjectsPage();
        expect(screen.getByText('Project Alpha')).toBeInTheDocument();
        expect(screen.getByText('Project Beta')).toBeInTheDocument();
        expect(screen.getByText('First project')).toBeInTheDocument();
        expect(screen.getByText('50%')).toBeInTheDocument(); // Progress for Alpha
        expect(screen.getByText('100%')).toBeInTheDocument(); // Progress for Beta
    });

    it('handles pagination', async () => {
        const user = userEvent.setup();
        (useProjects as any).mockReturnValue({
            isLoading: false,
            isError: false,
            data: mockProjectsData, // totalPages: 2
        });

        renderProjectsPage();

        const nextButton = screen.getByRole('button', { name: /next/i });
        await user.click(nextButton);

        // Ensure re-render happened with new page would be tricky without checking useProjects call args
        // But we can check that useProjects was called with page: 1 in subsequent renders if we could spy on it.
        await waitFor(() => {
            expect(useProjects).toHaveBeenCalledWith(expect.objectContaining({ page: 1 }));
        });
    });

    it('opens create project dialog and submits', async () => {
        const user = userEvent.setup();
        (useProjects as any).mockReturnValue({
            isLoading: false,
            isError: false,
            data: mockProjectsData,
        });
        mockCreateProjectMutate.mockResolvedValue({});

        renderProjectsPage();

        // Open Dialog
        await user.click(screen.getByRole('button', { name: /new project/i }));

        const dialogTitle = screen.getByRole('heading', { name: /create new project/i });
        expect(dialogTitle).toBeInTheDocument();

        // Fill Form
        await user.type(screen.getByLabelText(/project name/i), 'New Gamma Project');
        await user.type(screen.getByLabelText(/description/i), 'A newly created project');

        // Submit
        await user.click(screen.getByRole('button', { name: /create project/i })); // Note: "Create New Project" vs "Create Project" button inside dialog

        await waitFor(() => {
            expect(mockCreateProjectMutate).toHaveBeenCalledWith(expect.objectContaining({
                name: 'New Gamma Project',
                description: 'A newly created project',
            }));
            expect(mockToast).toHaveBeenCalledWith(expect.objectContaining({ title: 'Success' }));
        });
    });

    it('handles empty state', () => {
        (useProjects as any).mockReturnValue({
            isLoading: false,
            isError: false,
            data: { data: { content: [], totalPages: 0 } },
        });

        renderProjectsPage();
        expect(screen.getByText(/no projects found/i)).toBeInTheDocument();
    });
});
