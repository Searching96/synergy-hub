/**
 * Session Service
 * Handles API calls for Active Sessions management
 */

import api from "./api";
import type { ApiResponse } from "@/types/auth.types";
import type { Session } from "@/types/session.types";

export const sessionService = {
  /**
   * GET /users/me/sessions
   * Fetch all active sessions for the current user
   */
  getSessions: async (): Promise<ApiResponse<Session[]>> => {
    const response = await api.get<ApiResponse<Session[]>>("/users/me/sessions");
    return response.data;
  },

  /**
   * DELETE /users/me/sessions/{sessionId}
   * Revoke a specific session
   */
  revokeSession: async (sessionId: string): Promise<void> => {
    await api.delete(`/users/me/sessions/${sessionId}`);
  },

  /**
   * DELETE /users/me/sessions/other
   * Revoke all sessions except the current one
   */
  revokeAllOtherSessions: async (): Promise<void> => {
    await api.delete("/users/me/sessions/other");
  },
};
