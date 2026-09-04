import { apiRequest } from './client';
import type { Task } from './tasks';

export function fetchAlerts(): Promise<Task[]> {
  return apiRequest<Task[]>('/api/alerts');
}

export function dismissAlert(taskId: number): Promise<void> {
  return apiRequest<void>(`/api/alerts/${taskId}/dismiss`, { method: 'POST' });
}