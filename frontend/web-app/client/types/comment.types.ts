export interface Comment {
  id: number;
  content: string;
  createdAt: string;
  edited?: boolean;
  userId?: number;
  userName?: string;
  author?: {
    id: number;
    name: string;
    email?: string;
  } | null;
}
