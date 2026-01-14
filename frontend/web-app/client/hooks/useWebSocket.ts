import { useEffect, useRef, useCallback, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

interface UseWebSocketOptions {
    projectId: number | string;
    onMessage: (message: any) => void;
    enabled?: boolean;
}

export function useProjectChatWebSocket({ projectId, onMessage, enabled = true }: UseWebSocketOptions) {
    const clientRef = useRef<Client | null>(null);
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const connect = useCallback(() => {
        if (!enabled || !projectId) return;

        const token = localStorage.getItem('token');

        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            connectHeaders: {
                Authorization: token ? `Bearer ${token}` : '',
            },
            debug: (str) => {
                if (import.meta.env.DEV) {
                    console.log('[WebSocket]', str);
                }
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = () => {
            setIsConnected(true);
            setError(null);

            // Subscribe to project chat topic
            client.subscribe(`/topic/project/${projectId}/chat`, (message: IMessage) => {
                try {
                    const parsedMessage = JSON.parse(message.body);
                    onMessage(parsedMessage);
                } catch (e) {
                    console.error('Failed to parse WebSocket message:', e);
                }
            });
        };

        client.onDisconnect = () => {
            setIsConnected(false);
        };

        client.onStompError = (frame) => {
            console.error('STOMP error:', frame);
            setError(frame.headers?.message || 'WebSocket error');
            setIsConnected(false);
        };

        client.activate();
        clientRef.current = client;
    }, [projectId, onMessage, enabled]);

    const disconnect = useCallback(() => {
        if (clientRef.current) {
            clientRef.current.deactivate();
            clientRef.current = null;
            setIsConnected(false);
        }
    }, []);

    useEffect(() => {
        connect();
        return () => disconnect();
    }, [connect, disconnect]);

    return {
        isConnected,
        error,
        reconnect: connect,
    };
}
