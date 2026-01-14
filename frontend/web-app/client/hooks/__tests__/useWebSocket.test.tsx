import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useProjectChatWebSocket } from '../useWebSocket';
import { Client } from '@stomp/stompjs';

// Hoist mocks to be accessible inside vi.mock
const mocks = vi.hoisted(() => ({
    activate: vi.fn(),
    deactivate: vi.fn(),
    subscribe: vi.fn(),
}));

// We need a way to access the created client instances to trigger callbacks like onConnect
let clientInstances: any[] = [];

// Mock SockJS
vi.mock('sockjs-client', () => {
    return {
        default: vi.fn().mockImplementation(() => ({})),
    };
});

// Mock @stomp/stompjs
vi.mock('@stomp/stompjs', () => {
    return {
        Client: class {
            constructor() {
                clientInstances.push(this);
            }
            activate = mocks.activate;
            deactivate = mocks.deactivate;
            subscribe = mocks.subscribe;
            onConnect = () => { };
            onDisconnect = () => { };
            onStompError = () => { };
            connected = false;
        },
    };
});

describe('useProjectChatWebSocket', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        clientInstances = [];
    });

    it('activates client on mount', () => {
        const onMessage = vi.fn();
        renderHook(() => useProjectChatWebSocket({ projectId: 1, onMessage }));

        expect(mocks.activate).toHaveBeenCalled();
    });

    it('deactivates client on unmount', () => {
        const onMessage = vi.fn();
        const { unmount } = renderHook(() => useProjectChatWebSocket({ projectId: 1, onMessage }));

        unmount();
        expect(mocks.deactivate).toHaveBeenCalled();
    });

    it('subscribes to topic on connect', () => {
        const onMessage = vi.fn();
        renderHook(() => useProjectChatWebSocket({ projectId: 123, onMessage }));

        // Simulate connection
        act(() => {
            const client = clientInstances[0];
            if (client && client.onConnect) {
                client.onConnect();
            }
        });

        expect(mocks.subscribe).toHaveBeenCalledWith(
            '/topic/project/123/chat',
            expect.any(Function)
        );
    });

    it('handles incoming messages', () => {
        const onMessage = vi.fn();

        let messageCallback: any;
        mocks.subscribe.mockImplementation((_topic, callback) => {
            messageCallback = callback;
        });

        renderHook(() => useProjectChatWebSocket({ projectId: 1, onMessage }));

        // Connect
        act(() => {
            const client = clientInstances[0];
            if (client && client.onConnect) {
                client.onConnect();
            }
        });

        // Receive message
        const testPayload = { content: 'Hello' };
        act(() => {
            if (messageCallback) {
                messageCallback({ body: JSON.stringify(testPayload) });
            }
        });

        expect(onMessage).toHaveBeenCalledWith(testPayload);
    });
});
