export interface ActivityMetadata {
  taskTitle?: string;
  taskStatus?: string;
  [key: string]: unknown;
}

export interface ActivityEvent {
  id: number | string;
  eventType: string;
  eventDetails?: string;
  description?: string;
  metadata?: ActivityMetadata;
  actorName?: string;
  actorId?: number;
  systemEvent?: boolean;
  user?: {
    id: number;
    name: string;
    email?: string;
  };
  timestamp: string;
  ipAddress?: string;
}
