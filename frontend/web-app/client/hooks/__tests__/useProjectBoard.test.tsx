import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useProjectBoard } from '../useProjectBoard';
import { taskService } from '@/services/task.service';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactNode } from 'react';
import { toast } from 'sonner';

// Mock dependencies
vi.mock('@/services/task.service', () => ({
    taskService: {
        getProjectBoard: vi.fn(),
        updateTask: vi.fn(),
    },
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

// Mock Contexts
vi.mock('@/context/AuthContext', () => ({
    useAuth: () => ({ user: { id: 1, name: 'Test User' } }),
}));

vi.mock('@/context/ProjectContext', () => ({
    useProject: () => ({
        project: {
            members: [{ userId: 1, role: { permissions: [{ name: 'MOVE_TASK' }] } }]
        }
    }),
}));

vi.mock('@/lib/auth', () => ({
    canMoveTask: () => true,
}));

// Mock lib/error
vi.mock('@/lib/error', () => ({
    extractErrorMessage: () => 'Error occurred',
}));

// Mock RBAC util
vi.mock('@/lib/rbac', () => ({
    canMoveTask: () => true,
}));


const createTestQueryClient = () => new QueryClient({
    defaultOptions: {
        queries: {
            retry: false,
        },
    },
});

const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={createTestQueryClient()}>{children}</QueryClientProvider>
);

const mockBoardData = {
    activeSprints: [{
        id: 1,
        status: 'ACTIVE',
        name: 'Sprint 1',
        tasks: [
            { id: 101, title: 'Task 1', status: 'TO_DO' },
            { id: 102, title: 'Task 2', status: 'IN_PROGRESS' }
        ]
    }],
    backlogTasks: []
};

describe('useProjectBoard', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('fetches and organizes board data', async () => {
        (taskService.getProjectBoard as any).mockResolvedValue({ data: mockBoardData });

        const { result } = renderHook(() => useProjectBoard('1'), { wrapper });

        await waitFor(() => expect(result.current.isFetching).toBe(false));

        expect(result.current.activeSprint).toBeDefined();
        expect(result.current.data).toEqual(mockBoardData);
    });

    it('moves task successfully (optimistic update)', async () => {
        (taskService.getProjectBoard as any).mockResolvedValue({ data: mockBoardData });
        (taskService.updateTask as any).mockResolvedValue({ data: { id: 101, status: 'IN_PROGRESS' } });

        const { result } = renderHook(() => useProjectBoard('1'), { wrapper });
        await waitFor(() => expect(result.current.isFetching).toBe(false));

        // Call moveTask
        result.current.moveTask({
            taskId: 101,
            sourceStatus: 'TO_DO',
            sourceIndex: 0,
            destinationStatus: 'IN_PROGRESS',
            destinationIndex: 1
        });

        await waitFor(() => {
            expect(taskService.updateTask).toHaveBeenCalledWith(101, {
                status: 'IN_PROGRESS',
                position: 1
            });
        });
    });
});
