import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useSSOConfigs } from '../useSSOConfigs';
import { ssoService } from '@/services/sso.service';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { toast } from 'sonner';
import { ReactNode } from 'react';

// Mock dependencies
vi.mock('@/services/sso.service', () => ({
    ssoService: {
        getSsoProviders: vi.fn(),
        registerSsoProvider: vi.fn(),
        enableSsoProvider: vi.fn(),
        disableSsoProvider: vi.fn(),
        deleteSsoProvider: vi.fn(),
    },
}));

vi.mock('sonner', () => ({
    toast: {
        success: vi.fn(),
        error: vi.fn(),
    },
}));

vi.mock('axios');

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

describe('useSSOConfigs', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        // Mock localStorage for orgId
        vi.spyOn(Storage.prototype, 'getItem').mockReturnValue('1');
    });

    it('fetches SSO providers successfully', async () => {
        const mockProviders = [{ id: 1, providerName: 'Google', enabled: true }];
        (ssoService.getSsoProviders as any).mockResolvedValue(mockProviders);

        const { result } = renderHook(() => useSSOConfigs(1), { wrapper });

        await waitFor(() => expect(result.current.isLoadingProviders).toBe(false));
        expect(result.current.providers).toEqual(mockProviders);
    });

    it('registers new provider with optimistic update', async () => {
        const mockProviders = [];
        (ssoService.getSsoProviders as any).mockResolvedValue(mockProviders);
        (ssoService.registerSsoProvider as any).mockResolvedValue({ id: 2, providerName: 'GitHub', enabled: false });

        const { result } = renderHook(() => useSSOConfigs(1), { wrapper });

        await waitFor(() => expect(result.current.isLoadingProviders).toBe(false));

        result.current.register({
            providerName: 'GitHub',
            providerType: 'GITHUB',
            clientId: 'id',
            clientSecret: 'secret'
        });

        await waitFor(() => {
            expect(ssoService.registerSsoProvider).toHaveBeenCalled();
            expect(toast.success).toHaveBeenCalledWith(expect.stringContaining('registered successfully'));
        });
    });

    it('toggles provider status', async () => {
        const mockProviders = [{ id: 1, providerName: 'Google', enabled: true }];
        (ssoService.getSsoProviders as any).mockResolvedValue(mockProviders);
        (ssoService.disableSsoProvider as any).mockResolvedValue({ id: 1, providerName: 'Google', enabled: false });

        const { result } = renderHook(() => useSSOConfigs(1), { wrapper });
        await waitFor(() => expect(result.current.isLoadingProviders).toBe(false));

        result.current.toggle({ providerId: 1, enabled: false });

        await waitFor(() => {
            expect(ssoService.disableSsoProvider).toHaveBeenCalledWith(1);
            expect(toast.success).toHaveBeenCalledWith(expect.stringContaining('disabled'));
        });
    });
});
