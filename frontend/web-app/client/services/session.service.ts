/**
 * Session Service
 * Handles API calls for Active Sessions management
 */

import api from "./api";
import type { ApiResponse } from "@/types/auth.types";
import type { Session } from "@/types/session.types";

export const sessionService = {
  /**
   * GET /auth/sessions
   * Fetch all active sessions for the current user
   */
  getSessions: async (): Promise<ApiResponse<Session[]>> => {
    const response = await api.get<ApiResponse<Session[]>>("/auth/sessions");
    return response.data;
  },

  /**
   * DELETE /auth/sessions/{sessionId}
   * Revoke a specific session
   */
  revokeSession: async (sessionId: string): Promise<void> => {
    await api.delete(`/auth/sessions/${sessionId}`);
  },
};
