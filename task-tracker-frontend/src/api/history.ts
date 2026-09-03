import { apiRequest } from './client';

export type HistoryEventType = 'CREATED' | 'FIELD_CHANGED' | 'ASSIGNED' | 'UNASSIGNED' | 'COMMENT';

export interface HistoryEntry {
  id: number;
  actorEmail: string;
  eventType: HistoryEventType;
  fieldName: string | null;
  oldValue: string | null;
  newValue: string | null;
  commentText: string | null;
  createdAt: string;
}

export function fetchTaskHistory(taskId: number): Promise<HistoryEntry[]> {
  return apiRequest<HistoryEntry[]>(`/api/tasks/${taskId}/history`);
}

export function postComment(taskId: number, text: string): Promise<void> {
  return apiRequest<void>(`/api/tasks/${taskId}/comments`, { method: 'POST', body: { text } });
}