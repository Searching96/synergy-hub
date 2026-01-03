/**
 * Session Types for Active Sessions Feature
 * Matches SessionController DTOs from backend
 */

export interface Session {
  id: string;
  userId: number;
  userAgent: string;
  ipAddress: string;
  createdAt: string;
  lastAccessedAt: string;
  expiresAt: string;
  isCurrent: boolean;
}

export interface SessionListResponse {
  sessions: Session[];
  totalCount: number;
}
