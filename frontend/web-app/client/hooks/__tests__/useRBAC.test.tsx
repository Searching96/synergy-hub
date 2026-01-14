import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useRBAC } from '../useRBAC';
import { rbacService } from '@/services/rbac.service';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { toast } from 'sonner';
import { ReactNode } from 'react';
import axios from 'axios';

// Mock dependencies
vi.mock('@/services/rbac.service', () => ({
    rbacService: {
        getRoles: vi.fn(),
        getPermissions: vi.fn(),
        assignPermissionsToRole: vi.fn(),
        createRole: vi.fn(),
        deleteRole: vi.fn(),
    },
}));

vi.mock('sonner', () => ({
    toast: {
        success: vi.fn(),
        error: vi.fn(),
    },
}));

vi.mock('axios', () => ({
    default: {
        isAxiosError: (payload: any) => payload.isAxiosError === true,
    },
    isAxiosError: (payload: any) => payload.isAxiosError === true,
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

describe('useRBAC', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('fetches roles and permissions successfully', async () => {
        const mockRoles = [{ id: 1, name: 'Admin', permissions: [] }];
        const mockPermissions = [{ id: 101, name: 'READ_ALL', category: 'General' }];

        (rbacService.getRoles as any).mockResolvedValue(mockRoles);
        (rbacService.getPermissions as any).mockResolvedValue(mockPermissions);

        const { result } = renderHook(() => useRBAC(), { wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));

        expect(result.current.roles).toEqual(mockRoles);
        expect(result.current.permissions).toEqual(mockPermissions);
    });

    it('toggles permission successfully', async () => {
        const mockRoles = [{ id: 1, name: 'Editor', permissions: [] }];
        const mockPermissions = [{ id: 101, name: 'EDIT_POST', category: 'Content' }];

        (rbacService.getRoles as any).mockResolvedValue(mockRoles);
        (rbacService.getPermissions as any).mockResolvedValue(mockPermissions);
        (rbacService.assignPermissionsToRole as any).mockResolvedValue({});

        const { result } = renderHook(() => useRBAC(), { wrapper });

        await waitFor(() => expect(result.current.isLoading).toBe(false));

        // Toggle permission on
        result.current.togglePermission(1, 101);

        await waitFor(() => {
            expect(rbacService.assignPermissionsToRole).toHaveBeenCalledWith(1, {
                permissionIds: [101],
            });
        });
    });

    it('handles 403 Forbidden error', async () => {
        (rbacService.assignPermissionsToRole as any).mockRejectedValue({
            isAxiosError: true,
            response: { status: 403 },
        });

        const { result } = renderHook(() => useRBAC(), { wrapper });

        result.current.updatePermissions({ roleId: 1, permissionIds: [101] });

        await waitFor(() => {
            expect(toast.error).toHaveBeenCalledWith(
                expect.stringContaining("Access Denied")
            );
        });
    });

    it('creates a role successfully', async () => {
        (rbacService.createRole as any).mockResolvedValue({ id: 2, name: 'New Role' });

        const { result } = renderHook(() => useRBAC(), { wrapper });

        result.current.createRole({ name: 'New Role', description: 'Test' } as any);

        await waitFor(() => {
            expect(rbacService.createRole).toHaveBeenCalled();
            expect(toast.success).toHaveBeenCalledWith("Role created successfully!");
        });
    });
});
