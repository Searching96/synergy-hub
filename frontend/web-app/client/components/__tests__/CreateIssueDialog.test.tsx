import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import CreateIssueDialog from '../CreateIssueDialog';
import { useProjects } from '@/hooks/useProjects';
import { useCreateTask } from '@/hooks/useTasks';
import { projectService } from '@/services/project.service';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Mock dependencies
const mockToast = vi.fn();

vi.mock('@/hooks/use-toast', () => ({
    useToast: () => ({ toast: mockToast }),
}));

vi.mock('@/hooks/useProjects', () => ({
    useProjects: vi.fn(),
}));

vi.mock('@/hooks/useTasks', () => ({
    useCreateTask: vi.fn(),
}));

vi.mock('@/services/project.service', () => ({
    projectService: {
        getProjectMembers: vi.fn(),
    },
}));

// Mock Radix UI Select parts to simplify testing logic
vi.mock('@/components/ui/select', () => ({
    Select: ({ children, onValueChange, value }: any) => (
        <select
            data-testid="select-root"
            value={value}
            onChange={e => onValueChange(e.target.value)}
        >
            {children}
        </select>
    ),
    SelectTrigger: ({ children }: any) => <div>{children}</div>,
    SelectValue: () => null,
    SelectContent: ({ children }: any) => <>{children}</>,
    SelectItem: ({ children, value }: any) => <option value={value}>{children}</option>,
}));

// Mock Data
const mockProjects = [
    { id: 1, name: 'Project A', status: 'ACTIVE' },
    { id: 2, name: 'Project B', status: 'ACTIVE' },
];

const mockMembers = [
    { userId: 1, name: 'User One' },
    { userId: 2, name: 'User Two' },
];

const createTestQueryClient = () => new QueryClient({
    defaultOptions: {
        queries: {
            retry: false,
        },
    },
});

describe('CreateIssueDialog', () => {
    const mockOnOpenChange = vi.fn();
    const mockCreateTask = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();

        (useProjects as any).mockReturnValue({
            data: { data: { content: mockProjects } }
        });

        (useCreateTask as any).mockReturnValue({
            mutate: mockCreateTask,
            isPending: false,
        });

        (projectService.getProjectMembers as any).mockResolvedValue({
            data: mockMembers
        });
    });

    const renderWithClient = (ui: React.ReactElement) => {
        const testQueryClient = createTestQueryClient();
        return render(
            <QueryClientProvider client={testQueryClient}>
                {ui}
            </QueryClientProvider>
        );
    };

    it('renders nothing when closed', () => {
        renderWithClient(
            <CreateIssueDialog
                open={false}
                onOpenChange={mockOnOpenChange}
            />
        );
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('renders dialog content when open', () => {
        renderWithClient(
            <CreateIssueDialog
                open={true}
                onOpenChange={mockOnOpenChange}
            />
        );
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText('Create issue')).toBeInTheDocument();
    });

    it('submits form with valid data', async () => {
        const user = userEvent.setup();
        renderWithClient(
            <CreateIssueDialog
                open={true}
                onOpenChange={mockOnOpenChange}
            />
        );

        // Select Project (using our mock select)
        const projectSelect = screen.getAllByTestId('select-root')[0]; // First Select is Project
        await user.selectOptions(projectSelect, '1');

        // Type Title
        const titleInput = screen.getByPlaceholderText(/enter a concise summary/i);
        await user.type(titleInput, 'New Issue Title');

        // Select Type (Second select)
        const typeSelect = screen.getAllByTestId('select-root')[1];
        await user.selectOptions(typeSelect, 'TASK');

        // Click Create
        const createButton = screen.getByRole('button', { name: 'Create' });
        await user.click(createButton);

        await waitFor(() => {
            expect(mockCreateTask).toHaveBeenCalledWith(expect.objectContaining({
                projectId: 1,
                title: 'New Issue Title',
                type: 'TASK'
            }), expect.any(Object));
        });
    });

    it.skip('validates required fields', async () => {
        const user = userEvent.setup();
        renderWithClient(
            <CreateIssueDialog
                open={true}
                onOpenChange={mockOnOpenChange}
            />
        );

        // Wait for dialog content
        await screen.findByText('Create issue');

        // Try to submit without filling anything
        const submitButton = screen.getByRole('button', { name: 'Create' });

        // Use fireEvent to bypass potential overlay/pointer-events issues
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(mockToast).toHaveBeenCalledWith(expect.objectContaining({
                title: 'Error',
                description: expect.stringMatching(/select a project/i)
            }));
        });
    });

    it('handles create another checkbox', async () => {
        const user = userEvent.setup();
        renderWithClient(
            <CreateIssueDialog
                open={true}
                onOpenChange={mockOnOpenChange}
            />
        );

        // Check "Create another"
        const checkbox = screen.getByLabelText(/create another/i);
        await user.click(checkbox);

        expect(checkbox).toBeChecked();
    });
});
