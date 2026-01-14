import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useBacklogTasks, useMoveTaskToSprint, useUpdateTaskInline } from '../useBacklog';
import { taskService } from '@/services/task.service';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactNode } from 'react';

// Mock dependencies
vi.mock('@/services/task.service', () => ({
    taskService: {
        getProjectTasks: vi.fn(),
        moveTaskToSprint: vi.fn(),
        moveTaskToBacklog: vi.fn(),
        updateTask: vi.fn(),
    },
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

const mockBacklogContent = {
    content: [
        { id: 1, title: 'Task 1', sprintId: null },
        { id: 2, title: 'Task 2', sprintId: null }
    ]
};

describe('useBacklog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('useBacklogTasks', () => {
        it('fetches tasks successfully', async () => {
            (taskService.getProjectTasks as any).mockResolvedValue({ data: mockBacklogContent });

            const { result } = renderHook(() => useBacklogTasks('1'), { wrapper });

            await waitFor(() => expect(result.current.isSuccess).toBe(true));
            expect(result.current.data?.data).toEqual(mockBacklogContent);
        });
    });

    describe('useMoveTaskToSprint', () => {
        it('moves task to sprint successfully (optimistic)', async () => {
            (taskService.moveTaskToSprint as any).mockResolvedValue({ data: { success: true } });

            // Seed the query cache
            const queryClient = createTestQueryClient();
            queryClient.setQueryData(['backlog', '1'], { data: mockBacklogContent });

            const wrapperWithCache = ({ children }: { children: ReactNode }) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            );

            const { result } = renderHook(() => useMoveTaskToSprint('1'), { wrapper: wrapperWithCache });

            result.current.mutate({ taskId: 1, sprintId: 100 });

            await waitFor(() => {
                expect(taskService.moveTaskToSprint).toHaveBeenCalledWith(1, 100);
            });
        });

        it('moves task to backlog (null sprintId)', async () => {
            (taskService.moveTaskToBacklog as any).mockResolvedValue({ data: { success: true } });

            const { result } = renderHook(() => useMoveTaskToSprint('1'), { wrapper });

            result.current.mutate({ taskId: 1, sprintId: null });

            await waitFor(() => {
                expect(taskService.moveTaskToBacklog).toHaveBeenCalledWith(1);
            });
        });
    });

    describe('useUpdateTaskInline', () => {
        it('updates task successfully', async () => {
            (taskService.updateTask as any).mockResolvedValue({ data: { id: 1, title: 'Updated' } });

            const { result } = renderHook(() => useUpdateTaskInline('1'), { wrapper });

            result.current.mutate({ taskId: 1, updates: { title: 'Updated' } });

            await waitFor(() => {
                expect(taskService.updateTask).toHaveBeenCalledWith(1, { title: 'Updated' });
            });
        });
    });
});
